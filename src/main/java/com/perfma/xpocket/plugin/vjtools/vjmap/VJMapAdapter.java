package com.perfma.xpocket.plugin.vjtools.vjmap;

import com.perfma.xlab.xpocket.spi.process.XPocketProcess;
import com.perfma.xpocket.plugin.vjtools.vjmap.oops.HeapUtils;
import com.perfma.xpocket.plugin.vjtools.vjmap.oops.LoadedClassAccessor;
import com.perfma.xpocket.plugin.vjtools.vjmap.oops.OldgenAccessor;
import com.perfma.xpocket.plugin.vjtools.vjmap.oops.HeapHistogramVisitor;
import com.perfma.xpocket.plugin.vjtools.vjmap.oops.GenAddressAccessor;
import com.perfma.xpocket.plugin.vjtools.vjmap.oops.SurvivorAccessor;
import com.sun.tools.attach.VirtualMachine;
import com.perfma.xpocket.plugin.vjtools.vjmap.utils.TimeController.TimeoutException;
import sun.jvm.hotspot.HotSpotAgent;
import sun.jvm.hotspot.oops.ObjectHeap;
import sun.jvm.hotspot.runtime.VM;
import sun.tools.attach.HotSpotVirtualMachine;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;


public class VJMapAdapter {

	public static final String VERSION = "1.0.9";

	private static XPocketProcess tty;
	// 用于ctrl－C退出时仍然打印结果
	private static OldGenProcessor oldGenProcessor;
	private static HeapProcessor heapProcessor;

	public static void runHeapVisitor(int pid, boolean orderByName, long minSize) {
		ObjectHeap heap = VM.getVM().getObjectHeap();
		heapProcessor = new HeapProcessor(orderByName, minSize);

		tty.output("Iterating over heap. This may take a while...");
		tty.output("Geting live regions...");

		heap.iterate(heapProcessor.visitor);

		heapProcessor.printResult();
		heapProcessor = null;
	}

	public static class HeapProcessor {
		HeapHistogramVisitor visitor = new HeapHistogramVisitor(tty);
		boolean orderByName;
		long minSize;

		public HeapProcessor(boolean orderByName, long minSize) {
			this.orderByName = orderByName;
			this.minSize = minSize;
		}

		public void printResult() {
			List<ClassStats> list = HeapUtils.getClassStatsList(visitor.getClassStatsMap());
			ResultPrinter resultPrinter = new ResultPrinter();
			resultPrinter.printAllGens(tty, list, orderByName, minSize);
		}
	}

	public static void runSurviorAccessor(int age, int minAge, boolean orderByName, long minSize) {
		SurvivorAccessor accessor = new SurvivorAccessor();

		tty.output("Iterating over survivor area. This may take a while...");
		List<ClassStats> list = accessor.caculateHistogram(age, minAge,tty);

		ResultPrinter resultPrinter = new ResultPrinter();
		resultPrinter.printSurvivor(tty, list, orderByName, minSize, age, minAge);
	}

	public static void runOldGenAccessor(boolean orderByName, long minSize) {
		oldGenProcessor = new OldGenProcessor(orderByName, minSize);
		tty.output("Iterating over oldgen area. This may take a while...");
		oldGenProcessor.accessor.caculateHistogram(tty);
		oldGenProcessor.printResult();
		oldGenProcessor = null;
	}

	public static class OldGenProcessor {
		OldgenAccessor accessor = new OldgenAccessor();
		boolean orderByName;
		long minSize;

		public OldGenProcessor(boolean orderByName, long minSize) {
			this.orderByName = orderByName;
			this.minSize = minSize;
		}

		public void printResult() {
			List<ClassStats> list = HeapUtils.getClassStatsList(accessor.getClassStatsMap());
			ResultPrinter resultPrinter = new ResultPrinter();
			resultPrinter.printOldGen(tty, list, orderByName, minSize);
		}
	}

	public static void printGenAddress() {
		GenAddressAccessor accessor = new GenAddressAccessor();
		accessor.printHeapAddress(tty);
	}

	public static void printLoadedClass() {
		LoadedClassAccessor accessor = new LoadedClassAccessor();
		accessor.pringLoadedClass(tty);
	}

	public static void invoke(String[] args, XPocketProcess process) {
		tty = process;
		// 分析参数
		boolean orderByName = false;
		long minSize = -1;
		int minAge = 2;
		int age = -1;
		boolean live = false;
		// boolean dead = false;
		if (!(args.length == 2 || args.length == 3)) {
			printHelp();
			return;
		}

		String modeFlag = args[0];

		String[] modeFlags = modeFlag.split(":");
		if (modeFlags.length > 1) {
			String[] addtionalFlags = modeFlags[1].split(",");
			for (String addtionalFlag : addtionalFlags) {
				if ("byname".equalsIgnoreCase(addtionalFlag)) {
					orderByName = true;
				} else if (addtionalFlag.toLowerCase().startsWith("minsize")) {
					String[] values = addtionalFlag.split("=");
					if (values.length == 1) {
						tty.output("parameter " + addtionalFlag + " is wrong");
						return;
					}
					minSize = Long.parseLong(values[1]);
				} else if (addtionalFlag.toLowerCase().startsWith("minage")) {
					String[] values = addtionalFlag.split("=");
					if (values.length == 1) {
						tty.output("parameter " + addtionalFlag + " is wrong");
						return;
					}
					minAge = Integer.parseInt(values[1]);
				} else if (addtionalFlag.toLowerCase().startsWith("age")) {
					String[] values = addtionalFlag.split("=");
					if (values.length == 1) {
						tty.output("parameter " + addtionalFlag + " is wrong");
						return;
					}
					age = Integer.parseInt(values[1]);
				} else if (addtionalFlag.toLowerCase().startsWith("live")) {
					live = true;
				}
			}
		}

		Integer pid = null;
		String executablePath = null;
		String coredumpPath = null;
		if (args.length == 2) {
			pid = Integer.valueOf(args[1]);
		} else {
			executablePath = args[1];
			coredumpPath = args[2];
		}

		// 如有需要，执行GC
		if (live) {
			if (pid == null) {
				tty.output("only a running vm can be attached when live option is on");
				return;
			}
			triggerGc(pid);
		}


		//// 正式执行
		HotSpotAgent agent = new HotSpotAgent();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {

				// 如果ctrl＋C退出，仍尽量打印结果
				if (oldGenProcessor != null) {
					tty.output("VJMap aborted. Below is the incomplete summary: ");
					oldGenProcessor.printResult();
				}
				if (heapProcessor != null) {
					tty.output("VJMap aborted. Below is the incomplete summary: ");
					heapProcessor.printResult();
				}
			}
		});

		try {
			if (args.length == 2) {
				agent.attach(pid);
			} else {
				agent.attach(executablePath, coredumpPath);
			}

			long startTime = System.currentTimeMillis();
			if (modeFlag.startsWith("-all")) {
				runHeapVisitor(pid, orderByName, minSize);
			} else if (modeFlag.startsWith("-sur")) {
				runSurviorAccessor(age, minAge, orderByName, minSize);
			} else if (modeFlag.startsWith("-old")) {
				runOldGenAccessor(orderByName, minSize);
			} else if (modeFlag.startsWith("-address")) {
				printGenAddress();
			} else if (modeFlag.startsWith("-class")) {
				printLoadedClass();
			} else if (modeFlag.startsWith("-version")) {
				tty.output("vjmap version:" + VERSION);
				return;
			} else {
				printHelp();
				return;
			}
			long endTime = System.currentTimeMillis();
			double secs = (endTime - startTime) / 1000.0d;
			tty.output(String.format("%n Heap traversal took %.1f seconds.%n", secs));
		} catch (TimeoutException e) {
			tty.output("\n\nVJMap aborted by timeout.");
			tty.output("Try to use live option to reduce the fragments which make progress very slow.");
			tty.output("./vjmap -old:live PID\n\n");
		} catch (Exception e) {
			tty.output("Error Happen:" + e.getMessage());
			if (e.getMessage() != null && e.getMessage().contains("Can't attach to the process")) {
				tty.output(
						"Please use the same user of the target JVM to run vjmap, or use root user to run it (sudo -E vjmap ...)");
			}
		} finally {
			agent.detach();
		}
	}

	/**
	 * Trigger a remote gc using HotSpotVirtualMachine, inspired by jcmd's source code.
	 * 
	 * @param pid
	 */
	private static void triggerGc(Integer pid) {
		VirtualMachine vm = null;
		try {
			vm = VirtualMachine.attach(String.valueOf(pid));
			HotSpotVirtualMachine hvm = (HotSpotVirtualMachine) vm;
			try (InputStream in = hvm.executeJCmd("GC.run");) {
				byte b[] = new byte[256];
				int n;
				do {
					n = in.read(b);
					if (n > 0) {
						String s = new String(b, 0, n, "UTF-8");
						tty.output(s);
					}
				} while (n > 0);
				tty.output("");
			}
		} catch (Exception e) {
			tty.output(e.getMessage());
		} finally {
			if (vm != null) {
				try {
					vm.detach();
				} catch (IOException e) {
					tty.output(e.getMessage());
				}
			}
		}
	}

	private static void printHelp() {
		int leftLength = "-all:minsize=1024,byname".length();
		String format = " %-" + leftLength + "s  %s%n";
		tty.output("vjmap " + VERSION
				+ " - prints per GC generation (Eden, Survivor, OldGen) object details of a given process.");
		tty.output("Usage: vjmap <options> <PID>");
		tty.output("Usage: vjmap <options> <executable java path> <coredump file path>");
		tty.output("");
		tty.output(String.format(format,"-all", "print all gens histogram, order by total size"));
		tty.output(String.format(format,"-all:live", "print all gens histogram, live objects only"));
		tty.output(String.format(format,"-all:minsize=1024", "print all gens histogram, total size>=1024"));

		tty.output(String.format(format,"-all:minsize=1024,byname", "print all gens histogram, total size>=1024, order by class name"));

		tty.output(String.format(format,"-old", "print oldgen histogram, order by oldgen size"));
		tty.output(String.format(format,"-old:live", "print oldgen histogram, live objects only"));
		tty.output(String.format(format,"-old:minsize=1024", "print oldgen histogram, oldgen size>=1024"));
		tty.output(String.format(format,"-old:minsize=1024,byname", "print oldgen histogram, oldgen size>=1024, order by class name"));

		tty.output(String.format(format,"-sur", "print survivor histogram, age>=2"));
		tty.output(String.format(format,"-sur:age=4", "print survivor histogram, age==4"));
		tty.output(String.format(format,"-sur:minage=4", "print survivor histogram, age>=4, default is 2"));

		tty.output(String.format(format,"-sur:minsize=1024,byname", "print survivor histogram, age>=3, survivor size>=1024, order by class name"));
		tty.output(String.format(format,"-address", "print address for all gens"));
		tty.output(String.format(format,"-class", "print all loaded classes"));
	}
}
