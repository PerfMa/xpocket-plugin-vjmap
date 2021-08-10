package com.perfma.xpocket.plugin.vjtools.vjmap.utils;

import com.perfma.xlab.xpocket.spi.process.XPocketProcess;

import java.io.PrintStream;

public class ProgressNotifier {
	public long nextNotificationSize;
	public long processingSize;

	private int processingPercent;
	private long onePercentSize;
	private long totalSize;

	PrintStream printStream = System.out;

	private TimeController timeController = new TimeController();

	public ProgressNotifier(long totalSize) {
		this.totalSize = totalSize;
		onePercentSize = totalSize / 100;
		nextNotificationSize = onePercentSize;
		processingPercent = 0;
		processingSize = 0;
	}

	public void printHead(XPocketProcess tty) {
		tty.output("Total live size to process: " + FormatUtils.toFloatUnit(totalSize));
        printStream.print(" 0%:");
	}

	public void printProgress(XPocketProcess tty) {
		timeController.checkTimedOut();
        printStream.print(".");
		processingPercent++;
		nextNotificationSize += onePercentSize;
		if (processingPercent % 10 == 0) {
            printStream.print("\n" + processingPercent + "%:");
		}
	}
}
