package src;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

class DynamicInputDialog extends JDialog {
    private boolean confirmed = false;
    private HashMap<String, Double> inputValues;
    private int roomType;
    private HashMap<String, JTextField> textFields;
    private String[] roomTypes = {"Common room", "Bedroom", "Bathroom", "Kitchen"};
    JComboBox<String> typeComboBox = new JComboBox<>(roomTypes);

    public DynamicInputDialog(Frame owner, String title, String imagePath, String[] labels, int imageWidth, int imageHeight, String unit) {
        super(owner, title, true);
        setLayout(new BorderLayout());

        // Initialize the HashMaps
        textFields = new HashMap<>();
        inputValues = new HashMap<>();

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(mainPanel, BorderLayout.CENTER);

        // Image Panel
        JPanel imagePanel = new JPanel();
        ImageIcon originalIcon = new ImageIcon(imagePath);
        // Scale the image
        Image scaledImage = originalIcon.getImage().getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        JLabel imageLabel = new JLabel(scaledIcon);
        imagePanel.add(imageLabel);
        mainPanel.add(imagePanel, BorderLayout.NORTH);

        // Input Panel using GridBagLayout for flexibility
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Enter Values"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding between components
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int i = 0;
        for (i = 0; i < labels.length; i++) {
            String label = labels[i];
            JLabel jLabel = new JLabel(label + ":");
            JLabel unitLabel = new JLabel(unit);
            JTextField textField = new JTextField(5);
            ((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                    if (string.matches("\\d+")) { // Only allow digits
                        super.insertString(fb, offset, string, attr);
                    }
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attr) throws BadLocationException {
                    if (string.matches("\\d+")) { // Only allow digits
                        super.replace(fb, offset, length, string, attr);
                    }
                }
            });
            textField.setText(String.valueOf(1));
            // Store the text field in the HashMap
            textFields.put(label, textField);

            gbc.gridx = 0;
            gbc.gridy = i;
            inputPanel.add(jLabel, gbc);

            gbc.gridx = 1;
            inputPanel.add(textField, gbc);

            gbc.gridx = 2;
            inputPanel.add(unitLabel, gbc);
        }

        gbc.gridx = 0;
        gbc.gridy = i+1;
        JLabel comboLabel = new JLabel( "Room type:");
        inputPanel.add(comboLabel, gbc);

        
        typeComboBox.setSelectedItem("Common room");
        gbc.gridx = 1;
        gbc.gridy = i+1;
        inputPanel.add(typeComboBox, gbc);

        mainPanel.add(inputPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Action listener for OK button
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validateAndStoreInputs(labels)) {
                    confirmed = true;
                    dispose();
                }
            }
        });

        // Action listener for Cancel button
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                
                dispose();
            }
        });

        // Handle closing the dialog with the close button
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmed = false;
                dispose();
            }
        });

        // Adjust dialog size based on content
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private boolean validateAndStoreInputs(String[] labels) {
        inputValues.clear(); // Clear previous values
        roomType = 0;
        try {
            for (String label : labels) {
                String text = textFields.get(label).getText().trim();
                if (text.isEmpty()) {
                    throw new NumberFormatException("Empty input for " + label);
                } else if (Double.parseDouble(text) <= 0.0) {
                    throw new NumberFormatException("Non positive input for " + label);
                }
                double value = Double.parseDouble(text);
                inputValues.put(label, value);

                if(typeComboBox.getSelectedItem()=="Common room"){
                    roomType = 0;
                }
                else if(typeComboBox.getSelectedItem()=="Bedroom"){
                    roomType = 1;
                }
                else if(typeComboBox.getSelectedItem()=="Bathroom"){
                    roomType = 2;
                }
                else if(typeComboBox.getSelectedItem()=="Kitchen"){
                    roomType = 3;
                }
            
            }
            return true;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid positive values for all fields.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Checks if the dialog was confirmed (OK button was pressed).
     *
     * @return true if confirmed, false otherwise
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Retrieves the entered values as a HashMap.
     *
     * @return HashMap with labels as keys and entered double values.
     */
    public HashMap<String, Double> getInputValues() {
        return inputValues;
    }

    public int getRoomtype() {
        return roomType;
    }
}