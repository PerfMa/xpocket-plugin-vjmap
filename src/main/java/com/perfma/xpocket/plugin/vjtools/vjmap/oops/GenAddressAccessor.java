package com.perfma.xpocket.plugin.vjtools.vjmap.oops;

import com.perfma.xlab.xpocket.spi.process.XPocketProcess;
import com.perfma.xpocket.plugin.vjtools.vjmap.utils.VJMapPrint;
import sun.jvm.hotspot.gc_implementation.parallelScavenge.PSOldGen;
import sun.jvm.hotspot.gc_implementation.parallelScavenge.PSYoungGen;
import sun.jvm.hotspot.gc_interface.CollectedHeap;
import sun.jvm.hotspot.memory.ConcurrentMarkSweepGeneration;
import sun.jvm.hotspot.memory.DefNewGeneration;

import java.io.PrintStream;

/**
 * 打印各区地址
 */
public class GenAddressAccessor {

	public void printHeapAddress(XPocketProcess tty) {
		CollectedHeap heap = HeapUtils.getHeap();
		final VJMapPrint vjMapPrint = new VJMapPrint(System.out, tty);
		if (HeapUtils.isCMSGC(heap)) {
			DefNewGeneration youngGen = HeapUtils.getYoungGenForCMS(heap);
			youngGen.printOn(vjMapPrint);
			vjMapPrint.refresh();
			tty.output("");
			ConcurrentMarkSweepGeneration cmsGen = HeapUtils.getOldGenForCMS(heap);
			cmsGen.printOn(vjMapPrint);
			vjMapPrint.refresh();
		} else if (HeapUtils.isParallelGC(heap)) {
			// Parallel GC
			PSYoungGen psYoung = HeapUtils.getYongGenForPar(heap);
			psYoung.printOn(vjMapPrint);
			vjMapPrint.refresh();
			tty.output("");
			PSOldGen oldgen = HeapUtils.getOldGenForPar(heap);
			oldgen.printOn(vjMapPrint);
			vjMapPrint.refresh();
		} else {
			throw new IllegalArgumentException("Unsupport heap:" + heap.getClass().getName());
		}
	}
}
