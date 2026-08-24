package util;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;

public final class FileSaveDialog {

    private FileSaveDialog() {
    }

    public static String chooseXlsxPath(Frame parent, String defaultFileName) {
        FileDialog dialog = new FileDialog(parent, "Salvar arquivo", FileDialog.SAVE);
        dialog.setFile(defaultFileName);
        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return null;
        }

        String path = new File(dialog.getDirectory(), dialog.getFile()).getAbsolutePath();

        if (!path.toLowerCase().endsWith(".xlsx")) {
            path += ".xlsx";
        }

        return path;
    }
}