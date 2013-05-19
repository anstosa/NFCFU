package com.nfcfu.desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;

public class FileSelect extends JPanel implements ActionListener {
    DragAndDrop.DropPane parentPane;

    JButton openButton;
    JFileChooser fc;

    public FileSelect(DragAndDrop.DropPane parentPane) {
        super(new BorderLayout());

        this.parentPane = parentPane;

        //Create a file chooser
        fc = new JFileChooser();

        //Create the open button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        openButton = new JButton("Browse for them");
        openButton.addActionListener(this);

        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(openButton);

        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
    }

    public void actionPerformed(ActionEvent e) {
        int returnVal = fc.showOpenDialog(FileSelect.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();

            List<File> files = Arrays.asList(new File[]{file});
            parentPane.importFiles(files);
        }
    }
}
