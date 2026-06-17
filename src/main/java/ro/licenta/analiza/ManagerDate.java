package ro.licenta.analiza;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ManagerDate {
    public double[][] intrari;
    public double[][] tinte;
    public double[][] intrariValidare;
    public double[][] tinteValidare;
    public double maxVanzari = 50000;
    public double maxLuna = 12;
    public double maxSezon = 4;
    public double maxBuget = 10000;
    public double maxPret = 200;

    public void incarcaDate(String caleFisier) {
        List<double[]> inputList = new ArrayList<>();
        List<double[]> targetList = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(caleFisier));
                Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            int rows = sheet.getLastRowNum();
            if (rows > 0)
                maxVanzari = 0;
            for (int i = 1; i <= rows; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    try {
                        double vanzari = getCellValue(row, 5);
                        if (vanzari > maxVanzari)
                            maxVanzari = vanzari;
                    } catch (Exception e) {}
                }
            }

            System.out.println("DEBUG: Max Vanzari detected: " + maxVanzari); 

            // Safety check
            if (maxVanzari == 0)
                maxVanzari = 50000;

            for (int i = 1; i <= rows; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    try {
                        double luna = getCellValue(row, 1);
                        double sezon = getCellValue(row, 2);
                        double buget = getCellValue(row, 3);
                        double pret = getCellValue(row, 4);
                        double vanzari = getCellValue(row, 5);


                        // FIX: Skip rows with zero or invalid sales
                        if (vanzari <= 0.001) {
                            continue; 
                        }
 
                        // Normalizare (folosind maxVanzari detectat)

                        double[] in = new double[4];
                        in[0] = luna / 12.0;
                        in[1] = sezon / 4.0;
                        in[2] = buget / 10000.0;
                        in[3] = pret / 200.0;
                        double[] out = new double[1];
                        out[0] = vanzari / maxVanzari;

                        // DEBUG TRACE
                        // if (i < 10)
                        // System.out.println("DEBUG: Row " + i + " Vanzari=" + vanzari + " Norm=" +
                        // out[0]);
 
                        inputList.add(in);
                        targetList.add(out);
                    } catch (Exception ex) {
                        // Skip
                    }
                }
            }


            int totalVariabile = inputList.size();
            int nrAntrenare = (int) (totalVariabile * 0.80);
            int nrValidare = totalVariabile - nrAntrenare;

            java.util.List<Integer> indici = new java.util.ArrayList<>();
            for (int i = 0; i < totalVariabile; i++)
                indici.add(i);
            java.util.Collections.shuffle(indici, new java.util.Random(42));

            intrari = new double[nrAntrenare][];
            tinte = new double[nrAntrenare][];
            for (int i = 0; i < nrAntrenare; i++) {
                intrari[i] = inputList.get(indici.get(i));
                tinte[i] = targetList.get(indici.get(i));
            }

            intrariValidare = new double[nrValidare][];
            tinteValidare = new double[nrValidare][];
            for (int i = 0; i < nrValidare; i++) {
                intrariValidare[i] = inputList.get(indici.get(nrAntrenare + i));
                tinteValidare[i] = targetList.get(indici.get(nrAntrenare + i));
            }




            // Safety check if maxVanzari was not updated
            if (maxVanzari == 0)
                maxVanzari = 50000;

        } catch (

        Exception e) {
            System.err.println("Eroare la citirea datelor: " + e.getMessage());
            intrari = new double[0][0];
            tinte = new double[0][0];
        }
    }

    public void amestecaSetAntrenament() {
        if (intrari == null || intrari.length == 0)
            return;
        java.util.Random rnd = new java.util.Random();
        for (int i = intrari.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            double[] tempIn = intrari[index];
            intrari[index] = intrari[i];
            intrari[i] = tempIn;
            double[] tempOut = tinte[index];
            tinte[index] = tinte[i];
            tinte[i] = tempOut;
        }
    }

    private double getCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell != null) {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.FORMULA) {
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return 0.0;
                }
            } else if (cell.getCellType() == CellType.STRING) {
                try {
                    return Double.parseDouble(cell.getStringCellValue().replace(",", "."));
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        }
        return 0.0;
    }
}
