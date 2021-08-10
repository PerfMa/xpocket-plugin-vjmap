package com.perfma.xpocket.plugin.vjtools.vjmap.oops;

import com.perfma.xlab.xpocket.spi.process.XPocketProcess;
import com.perfma.xpocket.plugin.vjtools.vjmap.ClassStats;
import com.perfma.xpocket.plugin.vjtools.vjmap.utils.FormatUtils;
import com.perfma.xpocket.plugin.vjtools.vjmap.utils.VJMapPrint;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.debugger.OopHandle;
import sun.jvm.hotspot.gc_implementation.parallelScavenge.PSYoungGen;
import sun.jvm.hotspot.gc_implementation.shared.MutableSpace;
import sun.jvm.hotspot.gc_interface.CollectedHeap;
import sun.jvm.hotspot.memory.ContiguousSpace;
import sun.jvm.hotspot.memory.DefNewGeneration;
import sun.jvm.hotspot.oops.Klass;
import sun.jvm.hotspot.oops.ObjectHeap;
import sun.jvm.hotspot.oops.Oop;
import sun.jvm.hotspot.oops.UnknownOopException;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;

/**
 * 使用主动访问堆的方式统计Survivor区的对象信息, only support CMS and Parallel GC.
 * 
 * 迭代分区的代码，copy from sun.jvm.hotspot.oops.ObjectHeap.iterateLiveRegions()
 */
public class SurvivorAccessor {


	public List<ClassStats> caculateHistogram(int excactAge, int minAge, XPocketProcess tty) {
		HashMap<Klass, ClassStats> classStatsMap = new HashMap<>(2048, 0.2f);
		CollectedHeap heap = HeapUtils.getHeap();
		ObjectHeap objectHeap = HeapUtils.getObjectHeap();

		// 获取Survivor区边界
		Address fromBottom = null;
		Address fromTop = null;
		final VJMapPrint vjMapPrint = new VJMapPrint(System.out, tty);
		if (HeapUtils.isCMSGC(heap)) {
			DefNewGeneration youngGen = HeapUtils.getYoungGenForCMS(heap);
			ContiguousSpace from = youngGen.from();
			fromBottom = from.bottom();
			fromTop = from.top();

			from.printOn(vjMapPrint);
			vjMapPrint.refresh();
			tty.output("");
		} else if (HeapUtils.isParallelGC(heap)) {
			PSYoungGen psYoung = HeapUtils.getYongGenForPar(heap);
			MutableSpace from = psYoung.fromSpace();
			fromBottom = from.bottom();
			fromTop = from.top();

			from.printOn(vjMapPrint);
			vjMapPrint.refresh();
			tty.output("");
		} else {
			throw new IllegalArgumentException(
					"Only support CMS and Parallel GC. Unsupport heap:" + heap.getClass().getName());
		}

		// 记录分年龄统计
		long[] ageSize = new long[50];
		int[] ageCount = new int[50];
		int maxAge = 1;

		// 遍历Survivor区
		OopHandle handle = fromBottom.addOffsetToAsOopHandle(0);

		while (handle.lessThan(fromTop)) {
			Oop obj = null;
			try {
				obj = objectHeap.newOop(handle);
			} catch (UnknownOopException ex) {
				// ok
			}

			if (obj == null) {
				throw new UnknownOopException();
			}

			long objectSize = obj.getObjectSize();

			// handle指针指向下一个对象，后面的处理如果失败，直接进入下一个循环
			handle = handle.addOffsetToAsOopHandle(objectSize);

			Klass klass = obj.getKlass();
			if (klass == null) {
				continue;
			}

			int age = obj.getMark().age();

			ageCount[age]++;
			ageSize[age] += objectSize;
			if (age > maxAge) {
				maxAge = age;
			}

			// 如果设定了精确匹配age
			if (excactAge != -1) {
				if (age != excactAge) {
					continue;
				}
			} else if (age < minAge) {
				// 否则判断age>=minAge
				continue;
			}

			ClassStats stats = HeapUtils.getClassStats(klass, classStatsMap);
			stats.survivorCount++;
			stats.survivorSize += objectSize;
		}

		tty.output(String.format("%n#age    #count  #bytes%n"));

		for (int i = 1; i <= maxAge; i++) {
			tty.output(String.format("%3d: %9d %7s%n", i, ageCount[i], FormatUtils.toFloatUnit(ageSize[i])));

		}

		return HeapUtils.getClassStatsList(classStatsMap);
	}
}
