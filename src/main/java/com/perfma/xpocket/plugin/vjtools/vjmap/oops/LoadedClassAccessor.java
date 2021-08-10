package com.perfma.xpocket.plugin.vjtools.vjmap.oops;

import com.perfma.xlab.xpocket.spi.process.XPocketProcess;
import sun.jvm.hotspot.debugger.AddressException;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.oops.InstanceKlass;
import sun.jvm.hotspot.oops.Klass;
import sun.jvm.hotspot.oops.Oop;
import sun.jvm.hotspot.runtime.VM;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class LoadedClassAccessor {


	public void pringLoadedClass(XPocketProcess tty) {
		tty.output("Finding classes in System Dictionary..");

		try {
			final ArrayList<InstanceKlass> klasses = new ArrayList<>(128);

			SystemDictionary dict = VM.getVM().getSystemDictionary();
			dict.classesDo(new SystemDictionary.ClassVisitor() {
				@Override
				public void visit(Klass k) {
					if (k instanceof InstanceKlass) {
						klasses.add((InstanceKlass) k);
					}
				}
			});

			Collections.sort(klasses, new Comparator<InstanceKlass>() {
				@Override
				public int compare(InstanceKlass x, InstanceKlass y) {
					return x.getName().asString().compareTo(y.getName().asString());
				}
			});

			tty.output("#class             #loader");
			tty.output("-----------------------------------------------");
			for (InstanceKlass k : klasses) {
				tty.output(String.format("%s, %s", getClassNameFrom(k), getClassLoaderOopFrom(k)));
			}
		} catch (AddressException e) {
			tty.output("Error accessing address 0x" + Long.toHexString(e.getAddress()));
			e.printStackTrace();
		}
	}

	private static String getClassLoaderOopFrom(InstanceKlass klass) {
		Oop loader = klass.getClassLoader();
		return loader != null ? getClassNameFrom((InstanceKlass) loader.getKlass()) + " @ " + loader.getHandle()
				: "<bootstrap>";
	}

	private static String getClassNameFrom(InstanceKlass klass) {
		return klass != null ? klass.getName().asString().replace('/', '.') : null;
	}
}
