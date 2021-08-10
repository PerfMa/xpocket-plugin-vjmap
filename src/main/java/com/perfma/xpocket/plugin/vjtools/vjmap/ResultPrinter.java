package com.perfma.xpocket.plugin.vjtools.vjmap;

import com.perfma.xlab.xpocket.spi.process.XPocketProcess;
import com.perfma.xpocket.plugin.vjtools.vjmap.utils.FormatUtils;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ResultPrinter {

	/**
	 * 打印所有新老生代的结果
	 */
	public void printAllGens(XPocketProcess tty, List<ClassStats> list, boolean orderByName, long minSize) {

		if (orderByName) {
			Collections.sort(list, ClassStats.NAME_COMPARATOR);
		} else {
			Collections.sort(list, ClassStats.TOTAL_SIZE_COMPARATOR);
		}
		tty.output("\nObject Histogram:");
		tty.output("");
		tty.output(String.format("%6s %15s %15s %15s %15s  %s%n", "#num", "#all", "#eden", "#from", "#old", "#class description"));

		tty.output(
				"--------------------------------------------------------------------------------------------------");

		Iterator<ClassStats> iterator = list.listIterator();
		int num = 0;
		int totalCount = 0;
		int totalSize = 0;
		while (iterator.hasNext()) {
			ClassStats classStats = iterator.next();
			if (classStats.getSize() > minSize) {
				num++;
				totalCount = (int) (totalCount + classStats.getCount());
				totalSize = (int) (totalSize + classStats.getSize());

				tty.output(String.format("%5d: %7d/%7s %7d/%7s %7d/%7s %7d/%7s  %s%n", num, classStats.getCount(),
						FormatUtils.toFloatUnit(classStats.getSize()), classStats.getEdenCount(),
						FormatUtils.toFloatUnit(classStats.getEdenSize()), classStats.getSurvivorCount(),
						FormatUtils.toFloatUnit(classStats.getSurvivorSize()), classStats.getOldCount(),
						FormatUtils.toFloatUnit(classStats.getOldSize()), classStats.getDescription()));

			}
		}
		tty.output(String.format(" Total: %7d/%7s , minSize=%d%n", totalCount, FormatUtils.toFloatUnit(totalSize), minSize));
	}

	/**
	 * 打印只包含存活区的结果
	 */
	public void printSurvivor(XPocketProcess tty, List<ClassStats> list, boolean orderByName, long minSize, int age,
                              int minAge) {
		if (orderByName) {
			Collections.sort(list, ClassStats.NAME_COMPARATOR);
		} else {
			Collections.sort(list, ClassStats.SUR_SIZE_COMPARATOR);
		}

		tty.output("\nSurvivor Object Histogram:\n");
		tty.output(String.format("%6s %7s %7s  %s%n", "#num", "#count", "#bytes", "#Class description"));
		tty.output("-----------------------------------------------------------------------------------");

		Iterator<ClassStats> iterator = list.listIterator();
		int num = 0;
		long totalSurCount = 0;
		long totalSurSize = 0;
		while (iterator.hasNext()) {
			ClassStats classStats = iterator.next();
			if (classStats.getSurvivorSize() > minSize) {
				totalSurCount = totalSurCount + classStats.getSurvivorCount();
				totalSurSize = totalSurSize + classStats.getSurvivorSize();
				num++;
				tty.output(String.format("%5d: %7d %7s  %s%n", num, classStats.getSurvivorCount(),
						FormatUtils.toFloatUnit(classStats.getSurvivorSize()), classStats.getDescription()));
			}
		}

		if (age != -1) {
			tty.output(String.format(" Total: %7d %7s, age=%d, minSize=%d%n", totalSurCount, FormatUtils.toFloatUnit(totalSurSize),
					age, minSize));
		} else {
			tty.output(String.format(" Total: %7d %7s, minAge=%d, minSize=%d%n", totalSurCount, FormatUtils.toFloatUnit(totalSurSize),
					minAge, minSize));
		}
	}

	/**
	 * 打印只包含老生代的结果
	 */
	public void printOldGen(XPocketProcess tty, List<ClassStats> list, boolean orderByName, long minSize) {
		if (orderByName) {
			Collections.sort(list, ClassStats.NAME_COMPARATOR);
		} else {
			Collections.sort(list, ClassStats.OLD_SIZE_COMPARATOR);
		}

		tty.output("\nOldGen Object Histogram:\n");
		tty.output(String.format("%6s %7s %7s  %s%n", "#num", "#count", "#bytes", "#class description"));
		tty.output("-----------------------------------------------------------------------------------");

		Iterator<ClassStats> iterator = list.listIterator();
		int num = 0;
		long totalOldCount = 0;
		long totalOldSize = 0;
		while (iterator.hasNext()) {
			ClassStats classStats = iterator.next();
			if (classStats.getOldSize() > minSize) {
				totalOldCount = totalOldCount + classStats.getOldCount();
				totalOldSize = totalOldSize + classStats.getOldSize();
				num++;
				tty.output(String.format("%5d: %7s %7s  %s%n", num, classStats.getOldCount(),
						FormatUtils.toFloatUnit(classStats.getOldSize()), classStats.getDescription()));
			}
		}

		tty.output(String.format(" Total: %7d %7s, minSize=%d%n", totalOldCount, FormatUtils.toFloatUnit(totalOldSize), minSize));
	}

}
