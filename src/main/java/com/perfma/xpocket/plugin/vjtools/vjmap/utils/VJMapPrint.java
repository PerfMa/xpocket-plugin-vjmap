package com.perfma.xpocket.plugin.vjtools.vjmap.utils;

import com.perfma.xlab.xpocket.spi.process.XPocketProcess;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author xinxian
 * @create 2021-02-23 14:46
 **/
public class VJMapPrint extends PrintStream {

    private XPocketProcess tty;

    private StringBuffer stringBuffer;

    public VJMapPrint(OutputStream out,XPocketProcess tty) {
        super(out);
        this.tty = tty;
        this.stringBuffer = new StringBuffer();
    }

    @Override
    public void println(String x) {
        tty.output(x);
    }

    @Override
    public void print(String s) {
        stringBuffer.append(s);
    }

    @Override
    public void print(int i) {
        stringBuffer.append(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        stringBuffer.append(String.valueOf(l));
    }

    @Override
    public void print(double d) {
        stringBuffer.append(String.valueOf(d));
    }

    public void refresh() {
        tty.output(stringBuffer.toString());
        stringBuffer.setLength(0);
    }
}
