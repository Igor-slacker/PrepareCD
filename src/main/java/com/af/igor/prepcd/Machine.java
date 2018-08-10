package com.af.igor.prepcd;

import com.af.igor.prepcd.util.ConsoleHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by ede on 11.08.2016.
 */
public class Machine {
    protected final File luxFile;
    MainApp app = MainApp.getInstance();
    private final String machineName;
    private String machineType;
    private final String sm;        //first two symbols in machine name (usually 20)
    private final String machineSeries;
    private String remoteMachinePath;
    protected final String I_PLANS = app.PLANS + "002 - Plan d'installation/";
    private Path confFile;
    private Properties confFileProperty = new Properties();
    private String machineCode;
    private HashMap<String, String> propertyElements = new HashMap<>();

    MachineDir machineDir;
    CdDir cdDir;

    public MachineDir getMachineDir() {
        return machineDir;
    }

    public String getRemoteMachinePath() {
        return remoteMachinePath;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public boolean setRemoteMachinePath(String remoteMachinePath) {
        this.remoteMachinePath = remoteMachinePath;
        if (propertyElements.containsKey("remoteMachinePath"))
            propertyElements.put("remoteMachinePath", this.remoteMachinePath);
        return saveConfigFile();
    }

    public boolean setMachineCode(String machineCode) {
        this.machineCode = machineCode.equals("")?null:machineCode;     //if null, next call app.getMachineCode() calculates right code
        if (!propertyElements.containsKey("machineCode"))
            propertyElements.put("machineCode", this.machineCode);
        return saveConfigFile();
    }

    public Machine(String machineName) throws IOException {
        this.machineName = machineName;
        remoteMachinePath = app.H_MACHINES + machineName + "/";

        sm = machineName.substring(0, 2);
        machineSeries = machineName.substring(2, 3).equals("Y") ? machineName.substring(2, 3) : machineName.substring(2, 4);

        Path machineSeriesPath = Paths.get(app.MACHINES + sm + machineSeries);
        if (!Files.exists(machineSeriesPath))
            Files.createDirectory(machineSeriesPath);

        String luxPathString = remoteMachinePath + "010 Order/";
        luxFile = new File(app.searchFileName(luxPathString, machineName));
        if (luxFile == null) {
            app.logger.log("Program can't find luxemburg file\nluxPathString is:\n   " + luxPathString);
            app.logger.log("machineName is:\n   " + machineName);
            app.desktop.open(new File(luxPathString));
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                ConsoleHelper.writeMessage("Are you sure the machine " + machineName + " is exist? (y/N)");
                if (!reader.readLine().toLowerCase().equals("y")) machineName = null;
            }
        }

        if (machineName != null) {
            machineDir = new MachineDir(this);
            cdDir = new CdDir(this);

            confFile = Paths.get(machineDir.getMachinePath(), machineName + ".conf");
            loadFromConfigFile();
        }

    }

    private void loadFromConfigFile() {
        if (!Files.exists(confFile))
            return;

        try (FileInputStream stream = new FileInputStream(confFile.toFile())) {
            Properties confFileProperty = new Properties();

            confFileProperty.load(stream);
            if (confFileProperty.containsKey("remoteMachinePath")) {
                remoteMachinePath = confFileProperty.getProperty("remoteMachinePath");
                confFileProperty.setProperty("remoteMachinePath", remoteMachinePath);
                propertyElements.put("remoteMachinePath", remoteMachinePath);
            }
            if (confFileProperty.containsKey("machineCode")) {
                machineCode = confFileProperty.getProperty("machineCode");
                confFileProperty.setProperty("machineCode", machineCode);
                propertyElements.put("machineCode", machineCode);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean saveConfigFile() {
        boolean result = false;

        if (!Files.exists(confFile)) {
            try {
                Files.createFile(confFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileOutputStream stream = new FileOutputStream(confFile.toFile())) {
            for (Map.Entry<String, String> element : propertyElements.entrySet()) {
                if (element.getValue().equals(""))
                    confFileProperty.remove(element.getKey());
                else
                    confFileProperty.setProperty(element.getKey(), element.getValue());
            }
            confFileProperty.store(stream, null);
            result = true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
        app.logger.log("Machine type is: " + this.machineType);
    }

    public String getMachineType() {
        return machineType;
    }

    public String getMachineName() {
        return machineName;
    }

    public String getMachineSeries() {
        return machineSeries;
    }

    public String getSm() {
        return sm;
    }

    /*
    getMachineXls() check machineXls exist, if no copy and launch it in excel
    also copy Luxemburg xls and launch it too
     */
    public boolean getMachineXls() throws IOException {
        boolean isXlsCreated = false;
        if (!new File(machineDir.machinePath + machineDir.machineXls).exists()) {
            machineDir.copyXls();
            isXlsCreated = true;
        }
        app.logger.log("isXlsCreated is: " + isXlsCreated);
        return isXlsCreated;
    }

    public void prepareCd() throws IOException {
        String[] langs = app.machineExcelParser.getLanguage();
        for (String lang : langs) {
            ConsoleHelper.writeMessage(lang);
        }
//        new File(cdDir.cdPathDir).mkdir();
//        Files.copy(Paths.get(app.CDTEMPLATE),Paths.get(cdDir.cdPathDir));           //вибрати потрібні мови і файли
//        if (isEnglish)copyPlans();
//        else ...;
//        copyMinMop();
//        addCertif();
//        app.desktop.open(lang/index.html);
//        app.desktop.open(index.html);
    }

    public void openLuxFile() throws IOException {
        copyLuxFile();
        app.desktop.open(new File(machineDir.machinePath + luxFile));
    }

    public File getLuxFile() {
        return luxFile;
    }

    public String getXls() {
        return machineDir.machineXls;
    }

    public void renameAllCkd() throws IOException {
        String machineCode = app.getMachineCode();
        ArrayList<String> ckdFiles = machineDir.getCkdFiles();
        for (String file : ckdFiles) {
            if ((file.startsWith("E")) && (!file.startsWith("Etiqclas"))) {
                String renamedCkd = "E" + machineCode + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }
            if (file.startsWith("FS")) {
                String renamedCkd = "FS" + machineCode + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }

            if (file.startsWith("M10") &&
                    !file.startsWith("M100")) {
                String renamedCkd = "M10" + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }
            if (file.startsWith("M20")) {
                String renamedCkd = "M20" + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }
            if (file.startsWith("M30")) {
                String renamedCkd = "M30" + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }
            if (file.startsWith("M40")) {
                String renamedCkd = "M40" + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }
            if (file.startsWith("M50")) {
                String renamedCkd = "M50" + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }
            if (file.startsWith("M60")) {
                String renamedCkd = "M60" + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }
            if (file.startsWith("M70")) {
                String renamedCkd = "M70" + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }
            if (file.startsWith("M80")) {
                String renamedCkd = "M80" + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }
            if (file.startsWith("M90")) {
                String renamedCkd = "M90" + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }
            if (file.startsWith("M100")) {
                String renamedCkd = "M100" + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }
            if (file.startsWith("M110")) {
                String renamedCkd = "M110" + "-" + getMachineName().substring(2) + ".ckd";
                machineDir.rename(file, renamedCkd);
            }

        }
    }

    public void open4CkdFiles() throws IOException {
        ArrayList<String> ckdFiles = machineDir.getCkdFiles();
        for (String file : ckdFiles) {
            if (file.startsWith("FS")) app.desktop.open(new File(machineDir.machinePath + file));
            if (file.startsWith("Etiqclas")) app.desktop.open(new File(machineDir.machinePath + file));
            if ((file.startsWith("E")) && (!file.startsWith("Etiqclas")))
                app.desktop.open(new File(machineDir.machinePath + file));
            if (file.startsWith("I")) app.desktop.open(new File(machineDir.machinePath + file));
        }
    }

    public void copyEtiq() throws IOException {
        String machineEtiq = "Etiqclas" + "-" + getMachineName().substring(2) + ".ckd";
        if (!new File(machineDir.machinePath + machineEtiq).exists())
            machineDir.copy(app.ETIQCLAS, machineDir.machinePath + machineEtiq);
    }

    public void copyLuxFile() throws IOException {
        machineDir.copyLuxFile();
    }
}
