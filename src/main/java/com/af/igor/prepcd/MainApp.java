package com.af.igor.prepcd;

/* This program copy filtered .pdf files to cd's plan directory.
I..., E..., FS... plans rename to I,E,FS    M...-... -> M...pdf se, refr, hpet
First argument is a source directory path, second - target directory path. If argument only one it is a target path,
source path is a current directory. If there is not args, source path is a current directory and
target path is an appropriate cd directory.

else it can rename base .ckd to machine files, alike I...ckd -> I5500-GA1881.ckd
автоматичне створення каталога диску з подальшим копіюванням html файлів, файлів креслень, розпарсювання .xls файла
і автоматичного видалення зайвих директорій, відкриття пдф'ів інсттрукцій для ручного додаваня самих інструкцій.
 */

import com.af.igor.prepcd.log.SimpleLogger;
import com.af.igor.prepcd.util.LuxParser;
import com.af.igor.prepcd.util.MachineExcelParser;
import com.af.igor.prepcd.util.MachinesCode;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainApp {
    public static final String LOGFILE = "d:\\my_docs\\workDir\\PrepareCD.log";
    private static MainApp instance;
    private static String version = "0.3.1";
    private static Machine machine;         //in future this field will replace static ArrayList<Machine>
    static final String XLS = "d:\\my_docs\\workDir\\XL's\\20XX0000.xlsx";
    static final String MACHINES = "d:\\my_docs\\plans\\";
    static final String H_MACHINES = "h:\\DATAGEN\\Bt\\1.Client\\1.1.Machines\\";
    static final String CDS = "d:\\my_docs\\cdrom\\";
    static final String LUX_DIR = "k:\\Vente\\1. Customer\\1.3. Commandes\\";
    static final String CDTEMPLATE = "\\\\Serverua\\AF_UA\\1.4.CD\\WEB3_Operator Manual\\";
    static final String PLANS = "\\\\Serverua\\af_ua\\1.2.Plans\\";
    static Desktop desktop = Desktop.getDesktop();
    static final String totalCommander = "C:\\Program Files\\totalcmd\\TOTALCMD64";
    static public LuxParser luxParser;
    static MachineExcelParser machineExcelParser;
    public SimpleLogger logger;

    private MainApp() {}

    public static MainApp getInstance() {
        if (instance == null) {
            instance = new MainApp();
            instance.logger = new SimpleLogger();
            luxParser = LuxParser.getInstance();
            machineExcelParser = MachineExcelParser.getInstance();
            return instance;
        } else return instance;
    }

    public static Machine getMachine() {
        return machine;
    }

    void initializePath(String machineName) throws IOException, InterruptedException {
        /*
        Use current dir as path to machine
         */
        if (machineName == null) {
            Path path = Paths.get("").toAbsolutePath();
            if (!path.toString().startsWith(MACHINES)) {
                System.out.println("\nYou are not in \"plans\" directory");
                logger.log("You are not in \"plans\" directory");
                getInstance().help();
                return;
            }
            machineName = path.getFileName().toString().toUpperCase();
            logger.log("path got from pwd");
        }

        machine = new Machine(machineName);
        logger.log("machine name is: "+machineName);
    }

    private void help() throws InterruptedException {
        System.out.println("_________________\n" +
                "Available parameters:\n" +
                "-n [machine name]    Name of machine\n" +
                "-i                   Make installation\n" +
                "-x                   Create xls\n" +
                "-c                   Prepare cd");
        Thread.sleep(5000);
    }

    public static String getVersion() {
        return "\nPrepareCD version " + version;
    }

    public String searchFileName(String path, String pattern) {
        String fileName = null;
        int count = 0;
        try {
            String[] files = new File(path).list();
            for (String file : files) {
                if (file.startsWith(pattern)) {
                    fileName = file;
                    count++;
                }
            }
            if (count > 1) {
                desktop.open(new File(path));
                System.out.println("Attention! There are " + count + " files for machine " + machine.getMachineName() +
                        "\ncopy it manually from currently opened directory\n" +
                        "Already done? (y/N)");           //in future we can copy both (or many) files to machineDir and show message in window
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    while (reader.readLine().toLowerCase().equals("y")) {
                        count++;
                        if (count == 5) {
                            System.out.println("something wrong!");
                            fileName = null;
                            break;
//                        Thread.currentThread().stop();        // тут лежить какашка. Потрібно це зробити акуратніше
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.log("Error in mainApp.searchFileName\n   path is: " + path + "\n     pattern is: " + pattern+"\n");
            logger.log(e.toString());
            logger.stopLogging();
            e.printStackTrace();
        }
        return fileName;
    }

    public void tc(String parameters) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        String command = totalCommander + " /O " + parameters;
        Process process = runtime.exec(command);
        logger.log("total commander got parameters:\n   "+command);
    }

    public String getMachineCode() {
        String machineType = null;
        if (machine.getMachineType().contains("-"))
            machineType = machine.getMachineType().substring(0, machine.getMachineType().indexOf("-"));
        else machineType = machine.getMachineType();
        return MachinesCode.valueOf(machineType).toString();
    }

    public void run(String[] args) throws IOException, InterruptedException{
        /*
        available flags
        -x xls
        -n machine name
        -c cd
        -i installation
        -v version

        parsing args
        first of all we need to find -n flag and if it exist create machine instance with machine name as machine directory
        else we use current directory as machine dir
         */

        byte use = 0;
        String machineName = null;

        /*
        program version
         */
        for (int i = 0; i < args.length; i++) {
            String flag = args[i];
            if (flag.equals("-v")) {
                System.out.println(getVersion());
                logger.log("Showed version");
                logger.stopLogging();
                return;
            }
        }

        /*
        identify machine name
         */
        for (int i = 0; i < args.length; i++) {
            String flag = args[i];
            machineName = null;
            try {
                if (flag.equals("-n")) {
                    machineName = args[i + 1].toUpperCase();
                    if (machineName.startsWith("-")) throw new ArrayIndexOutOfBoundsException();
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("\nYou must specify the filename after -n flag");
                help();
                logger.log("Machine was not specify");
                logger.stopLogging();
                return;
            }
        }

        initializePath(machineName);
        if (machine == null) {
            logger.log("Error in mainApp.initializePath\n   machineName is: "+machineName);
            logger.stopLogging();
            return;
        }

        /*
        identify target
         */
        for (int i = 0; i < args.length; i++) {
            String flag = args[i];
            if (flag.equals("-x")) use = (byte) (use | 1);
            if (flag.equals("-c")) use = (byte) (use | 2);
            if (flag.equals("-i")) use = (byte) (use | 4);
        }


        /*
        use - marker control program:
        0 - do nothing
        1 - xls
        2 - cd
        3 - xls+cd
        4 - installation
         */
        if (use == 0) {
            logger.log("target isn't specified");
            System.out.println("\nYou must specify target. Application wont to know what to do");
            tc("/l=\"" + machine.machineDir.machinePath+"\"");
            help();
        }
        if (use == 1) {
            logger.log("target is: xls");
            if (!machine.getMachineXls()) {
                tc("/l=\"" + machine.machineDir.machinePath+"\" /t /r=\""+ machine.hMachinePath+"\"");
                desktop.open(new File(machine.machineDir.machinePath + machine.machineDir.machineXls));
                logger.log("xls is opened");
            }
            else {
                luxParser.setExcelFile(machine.machineDir.machinePath + machine.getLuxFile());
                machine.setMachineType(luxParser.getMachineType());
                luxParser.getMachineData();
                tc("/l=\"" + machine.machineDir.machinePath+"\" /t /r=\""+ machine.hMachinePath+"\"");
                desktop.open(new File(machine.machineDir.machinePath+machine.machineDir.machineXls));
                logger.log("xls is opened");
                desktop.open(new File(machine.machineDir.machinePath+machine.machineDir.luxFile));
                logger.log("lux is opened");
            }
        }
//        if (use == 2) machine.prepareCd();
//        if (use == 3) {
//            machine.getMachineXls();
//            machine.prepareCd();
//        }
        if (use == 4) {
            logger.log("target is: installation");
            machine.openLuxFile();
            luxParser.setExcelFile(machine.machineDir.machinePath + machine.getLuxFile());
            tc("/L=\"" + machine.machineDir.machinePath + "\" /T /R=\"" + machine.I_PLANS + "\"");
            logger.log("Opened in tc: \n   "+machine.machineDir.machinePath+"\n   "+ machine.I_PLANS );
            System.out.println("Copy base installation drawing\nAlready done? (press enter)");
            machine.setMachineType(luxParser.getMachineType());
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            String installationName = "I" + getMachineCode() + "-" + machine.getMachineName().substring(2) + ".ckd";
            Files.move(Paths.get(machine.machineDir.machinePath + machine.machineDir.getCkdFiles().get(0)), Paths.get(machine.machineDir.machinePath + installationName));  //rename installation
            desktop.open(new File(machine.machineDir.machinePath + installationName));
            logger.log("Installation "+ installationName+" opened");
            tc("/R=\"" + machine.hMachinePath + "\"");
            logger.log("Opened in tc :\n   "+machine.hMachinePath);
        }

        logger.stopLogging();
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        getInstance().run(args);
    }

}
