package com.perfma.xpocket.plugin.vjtools.vjmap.command;

import com.perfma.xlab.xpocket.spi.command.AbstractXPocketCommand;
import com.perfma.xlab.xpocket.spi.command.CommandInfo;
import com.perfma.xlab.xpocket.spi.process.XPocketProcess;
import com.perfma.xpocket.plugin.vjtools.vjmap.VJMapAdapter;

/**
 * @author xinxian
 * @create 2020-11-26 16:22
 **/
@CommandInfo(name = "vjmap", usage = "vjmap 1.0.9 - prints per GC generation (Eden, Survivor, OldGen) object details of a given process.")
public class VJMapCommand extends AbstractXPocketCommand {

    @Override
    public void invoke(XPocketProcess process) throws Throwable {
        final String[] args = process.getArgs();
        VJMapAdapter.invoke(args,process);
        process.end();
    }

    @Override
    public String details(String cmd) {
        return " Option                     Description                            \n" +
                "------                     -----------                            \n" +
                " -all                      print all gens histogram, order by total size\n" +
                " -all:live                 print all gens histogram, live objects only\n" +
                " -all:minsize=1024         print all gens histogram, total size>=1024\n" +
                " -all:minsize=1024,byname  print all gens histogram, total size>=1024, order by class name\n" +
                " -old                      print oldgen histogram, order by oldgen size\n" +
                " -old:live                 print oldgen histogram, live objects only\n" +
                " -old:minsize=1024         print oldgen histogram, oldgen size>=1024\n" +
                " -old:minsize=1024,byname  print oldgen histogram, oldgen size>=1024, order by class name\n" +
                " -sur                      print survivor histogram, age>=2\n" +
                " -sur:age=4                print survivor histogram, age==4\n" +
                " -sur:minage=4             print survivor histogram, age>=4, default is 2\n" +
                " -sur:minsize=1024,byname  print survivor histogram, age>=3, survivor size>=1024, order by class name\n" +
                " -address                  print address for all gens\n" +
                " -class                    print all loaded classes\n" +
                "Tips:\n" +
                " Usage: vjmap <options> <PID>\n" +
                " Usage: vjmap <options> <executable java path> <coredump file path>\n" +
                " Usage \"help vjmap\" show options info\n" +
                "Example:\n" +
                "打印整个堆中对象的统计信息，按对象的total size排序:\n" +
                "vjmap -all PID > /tmp/histo.log\n" +
                "\n" +
                "推荐，打印老年代的对象统计信息，按对象的oldgen size排序，比-all快很多，暂时只支持CMS:\n" +
                "vjmap -old PID > /tmp/histo-old.log\n" +
                "\n" +
                "推荐，打印Survivor区的对象统计信息，默认age>=3\n" +
                "vjmap -sur PID > /tmp/histo-sur.log\n" +
                "\n" +
                "推荐，打印Survivor区的对象统计信息，查看age>=4的对象\n" +
                "vjmap -sur:minage=4 PID > /tmp/histo-sur.log\n" +
                "\n" +
                "推荐，打印Survivor区的对象统计信息，单独查看age＝4的对象:\n" +
                "vjmap -sur:age=4 PID > /tmp/histo-sur.log";
    }
}
