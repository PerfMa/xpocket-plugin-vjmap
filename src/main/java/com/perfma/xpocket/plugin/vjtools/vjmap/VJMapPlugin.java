package com.perfma.xpocket.plugin.vjtools.vjmap;

import com.perfma.xlab.xpocket.spi.AbstractXPocketPlugin;
import com.perfma.xlab.xpocket.spi.process.XPocketProcess;

/**
 *
 * @author gongyu <yin.tong@perfma.com>
 */
public class VJMapPlugin extends AbstractXPocketPlugin {

    private static final String LOGO = " __     __      _   __  __      _      ____  \n" +
                                       " \\ \\   / /     | | |  \\/  |    / \\    |  _ \\ \n" +
                                       "  \\ \\ / /   _  | | | |\\/| |   / _ \\   | |_) |\n" +
                                       "   \\ V /   | |_| | | |  | |  / ___ \\  |  __/ \n" +
                                       "    \\_/     \\___/  |_|  |_| /_/   \\_\\ |_|    \n" +
                                       "                                           ";
    
    @Override
    public void printLogo(XPocketProcess process) {
        process.output(LOGO);
    }

    
    
}
