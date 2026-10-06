import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Static native target audit. Runtime Mixin transformation remains the authoritative check. */
public class NativeMixinAudit {
    static final Map<String, ClassNode> classes = new HashMap<>();
    static final List<Path> classpath = new ArrayList<>();
    static ClassNode read(String name) throws IOException {
        if (classes.containsKey(name)) return classes.get(name);
        for (var entry : classpath) {
            byte[] data = null;
            if (Files.isDirectory(entry)) {
                var file = entry.resolve(name + ".class");
                if (Files.isRegularFile(file)) data = Files.readAllBytes(file);
            } else if (Files.isRegularFile(entry) && entry.toString().endsWith(".jar")) {
                try (var zip = new ZipFile(entry.toFile())) {
                    var member = zip.getEntry(name + ".class");
                    if (member != null) try (var stream = zip.getInputStream(member)) { data = stream.readAllBytes(); }
                }
            }
            if (data != null) {
                var node = new ClassNode(); new ClassReader(data).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                classes.put(name, node); return node;
            }
        }
        classes.put(name, null); return null;
    }
    static List<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        var result = new ArrayList<AnnotationNode>(); if (visible != null) result.addAll(visible); if (invisible != null) result.addAll(invisible); return result;
    }
    static Object value(AnnotationNode ann, String key) {
        if (ann.values == null) return null;
        for (int i = 0; i < ann.values.size(); i += 2) if (ann.values.get(i).equals(key)) return ann.values.get(i + 1);
        return null;
    }
    static boolean methodExists(ClassNode node, String name, String descriptor, boolean parents) throws IOException {
        if (node == null) return false;
        for (var method : node.methods) if (method.name.equals(name) && (descriptor == null || method.desc.equals(descriptor))) return true;
        if (parents) {
            if (methodExists(read(node.superName), name, descriptor, true)) return true;
            for (var iface : node.interfaces) if (methodExists(read(iface), name, descriptor, true)) return true;
        }
        return false;
    }
    static boolean fieldExists(ClassNode node, String name, String descriptor) throws IOException {
        if (node == null) return false;
        for (var field : node.fields) if (field.name.equals(name) && field.desc.equals(descriptor)) return true;
        return fieldExists(read(node.superName), name, descriptor);
    }
    static String simple(String value) { return value.substring(value.lastIndexOf('/') + 1); }
    public static void main(String[] args) throws Exception {
        for (var entry : Files.readString(Path.of(args[0])).split(File.pathSeparator)) classpath.add(Path.of(entry));
        var root = Path.of(args[1]); int count = 0, diagnostics = 0;
        try (var stream = Files.walk(root)) {
            for (var file : stream.filter(p -> p.toString().endsWith(".class")).toList()) {
                var mixin = new ClassNode(); new ClassReader(Files.readAllBytes(file)).accept(mixin, 0);
                var annotation = annotations(mixin.visibleAnnotations, mixin.invisibleAnnotations).stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElse(null);
                if (annotation == null) continue;
                var targets = new ArrayList<String>();
                if (value(annotation, "value") instanceof List<?> list) for (var target : list) targets.add(((Type) target).getInternalName());
                if (value(annotation, "targets") instanceof List<?> list) for (var target : list) targets.add(target.toString().replace('.', '/'));
                for (var name : targets) {
                    count++; var target = read(name);
                    if (target == null) { System.out.println("MISSING CLASS " + simple(mixin.name) + " -> " + name); diagnostics++; continue; }
                    for (var field : mixin.fields) for (var ann : annotations(field.visibleAnnotations, field.invisibleAnnotations)) {
                        if (ann.desc.endsWith("/Shadow;") && !fieldExists(target, field.name, field.desc)) {
                            System.out.println("MISSING SHADOW " + simple(mixin.name) + ": " + field.name + " " + field.desc + " in " + name); diagnostics++;
                        }
                    }
                    for (var method : mixin.methods) for (var ann : annotations(method.visibleAnnotations, method.invisibleAnnotations)) {
                        String kind = ann.desc;
                        if (kind.endsWith("/Shadow;") || kind.endsWith("/Overwrite;")) {
                            if (!methodExists(target, method.name, method.desc, true)) {
                                System.out.println("MISSING MEMBER " + simple(mixin.name) + ": " + method.name + method.desc + " in " + name); diagnostics++;
                            }
                        } else if (kind.endsWith("/Accessor;") || kind.endsWith("/Invoker;")) {
                            // Runtime validates inferred accessor signatures and invoker field layouts.
                        } else if (value(ann, "method") != null) {
                            Object selectors = value(ann, "method"); List<?> list = selectors instanceof List<?> values ? values : List.of(selectors);
                            for (var selector : list) {
                                String selected = selector.toString(); if (selected.contains("*") || selected.startsWith("@")) continue;
                                int descriptor = selected.indexOf('(');
                                String selectedName = descriptor < 0 ? selected : selected.substring(0, descriptor);
                                String selectedDesc = descriptor < 0 ? null : selected.substring(descriptor);
                                if (!methodExists(target, selectedName, selectedDesc, false)) {
                                    System.out.println("MISSING TARGET " + simple(mixin.name) + ": " + selected + " in " + name); diagnostics++;
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Audited " + count + " native mixin targets; " + diagnostics + " diagnostics (including members added by other mixins).");
    }
}
