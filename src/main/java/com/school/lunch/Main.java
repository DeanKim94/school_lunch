package com.school.lunch;

import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.school.lunch.ui.MainFrame;

import javax.swing.*;
import java.awt.Font;

public class Main {
    public static void main(String[] args) {
        // Set FlatLaf look and feel (Mac style for premium look)
        try {
            UIManager.put("defaultFont", new Font("Malgun Gothic", Font.PLAIN, 15));
            UIManager.setLookAndFeel(new FlatMacLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Failed to initialize LaF");
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
