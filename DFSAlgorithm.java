import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

public class DFSAlgorithm {

    // Gradient color constants
    private static final Color GRADIENT_COLOR1 = new Color(255, 255, 255);  // Steel blue
    private static final Color GRADIENT_COLOR2 = new Color(57, 57, 211);   // Dark blu

    // Gradient color constants for log panel
    private static final Color LOG_GRADIENT_START = new Color(148, 148, 207, 152);  // Dark blue
    private static final Color LOG_GRADIENT_END = new Color(255, 255, 255);    // Medium blue

    private static int[][] adjacencyMatrix;
    private static JFrame mainFrame;
    private static Graph graph;
    private static Viewer viewer;
    private static boolean isVisualizingDFS = false;
    private static javax.swing.Timer dfsTimer;
    private static List<DFSStep> dfsTraversalOrder;
    private static int currentDfsStep = 0;
    private static JPanel inputPanel;
    private static String selectedSource = null;
    private static String selectedDestination = null;
    private static int visualizationSpeed = 1000;
    private static JComboBox<String> sourceComboBox;
    private static JComboBox<String> destinationComboBox;
    private static JPanel graphPanel;
    private static JButton addNodeButton;
    private static JButton removeNodeButton;
    private static JButton addEdgeButton;
    private static JButton removeEdgeButton;
    private static JSpinner edgeWeightSpinner;
    private static JTextArea logTextArea;
    private static JScrollPane logScrollPane;
    private static JButton pauseButton;
    private static JButton resumeButton;

    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui", "swing");
        SwingUtilities.invokeLater(() -> {
            createMainWindow();
            System.out.println("Application started successfully");
        });
    }

    private static void createMainWindow() {
        mainFrame = new JFrame("Graph Visualizer with DFS");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1000, 700);
        mainFrame.setLayout(new BorderLayout());

        JPanel startPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_COLOR1, getWidth(), getHeight(), GRADIENT_COLOR2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        // Add the text label
        JLabel fullScreenLabel = new JLabel("For the best viewing experience, please switch to full-screen mode.");
        fullScreenLabel.setFont(new Font("Arial", Font.BOLD, 19));
        fullScreenLabel.setForeground(Color.RED);
        fullScreenLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton startButton = createStyledButton("START GRAPH VISUALIZER", 24);
        startButton.addActionListener(e -> showMainOptions());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Add the label first
        startPanel.add(fullScreenLabel, gbc);
        // Then add the button
        startPanel.add(startButton, gbc);

        mainFrame.add(startPanel, BorderLayout.CENTER);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static JButton createStyledButton(String text, int fontSize) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(33, 76, 170));
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(4, 33, 89));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(33, 76, 170));
            }
        });

        return button;
    }

    private static void showMainOptions() {
        mainFrame.getContentPane().removeAll();

        // Create a panel with gradient background
        JPanel optionsPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_COLOR1, getWidth(), getHeight(), GRADIENT_COLOR2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        optionsPanel.setBorder(new EmptyBorder(50, 100, 50, 100));

        // Create buttons with fixed width but same font size
        JButton manualButton = createOptionButtonWithFixedWidth("Manual Input", 30, 700);
        JButton randomButton = createOptionButtonWithFixedWidth("Generate Random", 30, 700);
        JButton exitButton = createOptionButtonWithFixedWidth("Exit", 30, 700);

        manualButton.addActionListener(e -> showManualInputPanel());
        randomButton.addActionListener(e -> {
            // Generate random graph with random parameters
            Random rand = new Random();
            int nodes = 5 + rand.nextInt(46); // 5-50 nodes (changed from 5-10)
            int probability = 30 + rand.nextInt(71); // 30-100% probability
            int maxWeight = 1 + rand.nextInt(20); // 1-20 weight

            generateRandomGraph(nodes, probability, maxWeight);
            visualizeGraph();

            // Select random source and destination
            int source = rand.nextInt(nodes);
            int destination;
            do {
                destination = rand.nextInt(nodes);
            } while (destination == source);

            selectedSource = "N" + source;
            selectedDestination = "N" + destination;
            sourceComboBox.setSelectedItem(selectedSource);
            destinationComboBox.setSelectedItem(selectedDestination);
            highlightSelectedNodes();

            logMessage("[Random] Generated graph with " + nodes + " nodes");
            logMessage("[Random] Selected source: " + selectedSource + ", destination: " + selectedDestination);

            // Check if graph is disconnected
            if (isGraphDisconnected()) {
                logMessage("[Warning] Generated graph is disconnected - some nodes may not be reachable");
            }

        });

        exitButton.addActionListener(e -> System.exit(0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(20, 10, 20, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        optionsPanel.add(manualButton, gbc);
        optionsPanel.add(randomButton, gbc);
        optionsPanel.add(exitButton, gbc);

        // Back panel with gradient background
        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_COLOR1, getWidth(), getHeight(), GRADIENT_COLOR2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backPanel.setOpaque(false);

        JButton backButton = createStyledButton("Back", 14);
        backButton.addActionListener(e -> createMainWindow());
        backPanel.add(backButton);

        mainFrame.add(backPanel, BorderLayout.NORTH);
        mainFrame.add(optionsPanel, BorderLayout.CENTER);
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private static JButton createOptionButtonWithFixedWidth(String text, int fontSize, int width) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(33, 76, 170));
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(width, button.getPreferredSize().height));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(4, 33, 89));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(33, 76, 170));
            }
        });

        return button;
    }

    private static JButton createOptionButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(33, 76, 170));
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(4, 33, 89));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(33, 76, 170));
            }
        });

        return button;
    }

    private static void showManualInputPanel() {
        // Create a panel with gradient background
        inputPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_COLOR1, getWidth(), getHeight(), GRADIENT_COLOR2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        inputPanel.setBorder(new EmptyBorder(50, 100, 50, 100));

        // Create a centered panel for the controls
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false); // Make transparent to show gradient

        JLabel nodesLabel = new JLabel("Number of nodes (2-50):");
        nodesLabel.setForeground(Color.BLACK);
        nodesLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JSpinner nodesSpinner = new JSpinner(new SpinnerNumberModel(5, 2, 50, 1));
        nodesSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        styleSpinner(nodesSpinner);

        JLabel probLabel = new JLabel("Edge probability (0%-150%):");
        probLabel.setForeground(Color.BLACK);
        probLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JSpinner probSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 150, 5));
        probSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        styleSpinner(probSpinner);

        JLabel weightLabel = new JLabel("Max edge weight (1-100):");
        weightLabel.setForeground(Color.BLACK);
        weightLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JSpinner weightSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        weightSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        styleSpinner(weightSpinner);

        JButton generateButton = createStyledButton("Generate and Visualize", 30);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(15, 10, 15, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        centerPanel.add(nodesLabel, gbc);
        centerPanel.add(nodesSpinner, gbc);
        centerPanel.add(probLabel, gbc);
        centerPanel.add(probSpinner, gbc);
        centerPanel.add(weightLabel, gbc);
        centerPanel.add(weightSpinner, gbc);
        centerPanel.add(Box.createVerticalStrut(2), gbc);
        centerPanel.add(generateButton, gbc);

        inputPanel.add(centerPanel, BorderLayout.CENTER);

        generateButton.addActionListener(e -> {
            int nodes = (Integer) nodesSpinner.getValue();
            int probability = (Integer) probSpinner.getValue();
            int maxWeight = (Integer) weightSpinner.getValue();

            generateRandomGraph(nodes, probability, maxWeight);
            visualizeGraph();

            // Let user select source and destination
            logMessage("[Manual] Generated graph with " + nodes + " nodes");
            logMessage("[Manual] Please select source and destination nodes");

            // Check if graph is disconnected
            if (isGraphDisconnected()) {
                logMessage("[Warning] Generated graph is disconnected - some nodes may not be reachable");
            }
        });

        // Back panel with gradient background
        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_COLOR1, getWidth(), getHeight(), GRADIENT_COLOR2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backPanel.setOpaque(false);

        JButton backButton = createStyledButton("Back", 14);
        backButton.addActionListener(e -> showMainOptions());
        backPanel.add(backButton);

        mainFrame.getContentPane().removeAll();
        mainFrame.add(backPanel, BorderLayout.NORTH);
        mainFrame.add(inputPanel, BorderLayout.CENTER);
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private static void styleSpinner(JSpinner spinner) {
        spinner.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        spinner.setBackground(new Color(240, 240, 240));
        spinner.setOpaque(true);

        // Style the spinner buttons
        Component editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setColumns(5);
            tf.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            tf.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }
    }

    private static void generateRandomGraph(int nodes, int probability, int maxWeight) {
        Random rand = new Random();
        adjacencyMatrix = new int[nodes][nodes];

        for (int i = 0; i < nodes; i++) {
            for (int j = i + 1; j < nodes; j++) {
                if (rand.nextInt(100) < probability) {
                    int weight = 1 + rand.nextInt(maxWeight);
                    adjacencyMatrix[i][j] = weight;
                    adjacencyMatrix[j][i] = weight;
                }
            }
        }

        for (int i = 0; i < nodes; i++) {
            adjacencyMatrix[i][i] = 0;
        }
    }

    private static boolean isGraphDisconnected() {
        if (adjacencyMatrix == null || adjacencyMatrix.length == 0) return true;

        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(0);
        visited.add(0);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (int i = 0; i < adjacencyMatrix.length; i++) {
                if (adjacencyMatrix[current][i] > 0 && !visited.contains(i)) {
                    visited.add(i);
                    queue.add(i);
                }
            }
        }

        return visited.size() != adjacencyMatrix.length;
    }

    private static void visualizeGraph() {
        if (adjacencyMatrix == null || adjacencyMatrix.length == 0) {
            JOptionPane.showMessageDialog(mainFrame, "No graph data to visualize", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Clear the main frame
        mainFrame.getContentPane().removeAll();

        // Initialize combo boxes
        sourceComboBox = new JComboBox<>();
        destinationComboBox = new JComboBox<>();
        populateNodeComboBoxes();

        // Panel for source and destination selection
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectionPanel.add(new JLabel("Source:"));
        selectionPanel.add(sourceComboBox);
        selectionPanel.add(new JLabel("Destination:"));
        selectionPanel.add(destinationComboBox);

        // Panel for topology modification controls
        JPanel topologyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Add Node button
        addNodeButton = new JButton("Add Node");
        addNodeButton.addActionListener(e -> addNewNode());

        // Remove Node button
        removeNodeButton = new JButton("Remove Node");
        removeNodeButton.addActionListener(e -> removeSelectedNode());

        // Add Edge components
        JPanel addEdgePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addEdgePanel.add(new JLabel("Edge Weight:"));
        edgeWeightSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        addEdgePanel.add(edgeWeightSpinner);
        addEdgeButton = new JButton("Add Edge");
        addEdgeButton.addActionListener(e -> addNewEdge());

        // Remove Edge button
        removeEdgeButton = new JButton("Remove Edge");
        removeEdgeButton.addActionListener(e -> removeSelectedEdge());

        // Add to the selectionPanel in visualizeGraph() method
        JButton selectButton = new JButton("Select");
        selectButton.addActionListener(e -> {
            if (sourceComboBox.getSelectedItem() != null && destinationComboBox.getSelectedItem() != null) {
                selectedSource = (String) sourceComboBox.getSelectedItem();
                selectedDestination = (String) destinationComboBox.getSelectedItem();
                highlightSelectedNodes();
                logMessage("[Selection] Source set to " + selectedSource + ", Destination set to " + selectedDestination);
            } else {
                JOptionPane.showMessageDialog(mainFrame,
                        "Please select both source and destination nodes first");
            }
        });

        selectionPanel.add(selectButton);

        topologyPanel.add(addNodeButton);
        topologyPanel.add(removeNodeButton);
        topologyPanel.add(addEdgePanel);
        topologyPanel.add(addEdgeButton);
        topologyPanel.add(removeEdgeButton);

        // Panel for algorithm buttons
        JPanel algorithmPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton dfsButton = createOptionButton("Run DFS");
        JButton resetButton = createOptionButton("Reset Graph");
        JButton backButton = new JButton("Back to Main Menu");

        // Add pause and resume buttons
        pauseButton = new JButton("Pause");
        resumeButton = new JButton("Resume");
        resumeButton.setEnabled(false);

        pauseButton.addActionListener(e -> {
            if (dfsTimer != null && dfsTimer.isRunning()) {
                dfsTimer.stop();
                pauseButton.setEnabled(false);
                resumeButton.setEnabled(true);
                logMessage("[Visualization] Paused DFS visualization");
            }
        });

        resumeButton.addActionListener(e -> {
            if (dfsTimer != null && !dfsTimer.isRunning()) {
                dfsTimer.start();
                pauseButton.setEnabled(true);
                resumeButton.setEnabled(false);
                logMessage("[Visualization] Resumed DFS visualization");
            }
        });

        dfsButton.addActionListener(e -> {
            selectedSource = (String) sourceComboBox.getSelectedItem();
            selectedDestination = (String) destinationComboBox.getSelectedItem();
            if (selectedSource != null && selectedDestination != null) {
                logMessage("\n[Algorithm] Starting DFS traversal from " + selectedSource + " to " + selectedDestination);
                startDFSTraversal();
                pauseButton.setEnabled(true);
                resumeButton.setEnabled(false);
            } else {
                JOptionPane.showMessageDialog(mainFrame,
                        "Please select both source and destination nodes first!");
            }
        });

        resetButton.addActionListener(e -> {
            logMessage("[Reset] Graph visualization reset");
            resetGraphColors();
            selectedSource = null;
            selectedDestination = null;
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
        });

        backButton.addActionListener(e -> showMainOptions());

        algorithmPanel.add(dfsButton);
        algorithmPanel.add(resetButton);
        algorithmPanel.add(pauseButton);
        algorithmPanel.add(resumeButton);
        algorithmPanel.add(backButton);

        // Add speed control to the control panel
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel speedLabel = new JLabel("Visualization Speed:");
        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 100, 1000, 800);
        speedSlider.setMajorTickSpacing(300);
        speedSlider.setMinorTickSpacing(100);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setSnapToTicks(true);

// Create a label to show the current speed value
        JLabel speedValueLabel = new JLabel("800 ms");
        speedValueLabel.setPreferredSize(new Dimension(60, 20));

        speedSlider.addChangeListener(e -> {
            visualizationSpeed = speedSlider.getValue();
            speedValueLabel.setText(visualizationSpeed + " ms");

            // Update timer if it's running
            if (dfsTimer != null && dfsTimer.isRunning()) {
                dfsTimer.setDelay(visualizationSpeed);
            }
            logMessage("[Settings] Visualization speed set to " + visualizationSpeed + " ms");
        });

        speedPanel.add(speedLabel);
        speedPanel.add(speedSlider);
        speedPanel.add(speedValueLabel);

        // Add components to control panel
        JPanel topControlPanel = new JPanel(new GridLayout(2, 1));
        topControlPanel.add(selectionPanel);
        topControlPanel.add(topologyPanel);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        controlPanel.add(topControlPanel, BorderLayout.NORTH);
        controlPanel.add(speedPanel, BorderLayout.CENTER);
        controlPanel.add(algorithmPanel, BorderLayout.SOUTH);

        // Create the graph panel
        graphPanel = new JPanel(new BorderLayout());

        // Initialize the graph
        if (viewer != null) {
            viewer.close();
        }

        graph = new SingleGraph("Graph Visualization");
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");
        graph.setAttribute("ui.stylesheet", getStylesheet());

        // Create nodes
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            String nodeId = "N" + i;
            graph.addNode(nodeId);
            graph.getNode(nodeId).setAttribute("ui.label", nodeId);
            graph.getNode(nodeId).setAttribute("ui.style", "fill-color: lightblue;");
        }

        // Create edges
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            for (int j = i; j < adjacencyMatrix[i].length; j++) {
                if (adjacencyMatrix[i][j] > 0) {
                    String edgeId = "E" + i + "-" + j;
                    graph.addEdge(edgeId, "N" + i, "N" + j, i > j)
                            .setAttribute("ui.label", adjacencyMatrix[i][j]);
                }
            }
        }

        // Reset selections
        selectedSource = null;
        selectedDestination = null;

        // Set up the viewer
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        View view = viewer.addDefaultView(false);
        viewer.enableAutoLayout();

        // Add the view to the graph panel
        graphPanel.add((Component) view, BorderLayout.CENTER);

        // Create a main content panel with BorderLayout
        JPanel mainContentPanel = new JPanel(new BorderLayout());

        // Create a panel for graph and controls (left side)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(controlPanel, BorderLayout.NORTH);
        leftPanel.add(graphPanel, BorderLayout.CENTER);

        // Create log panel (right side) with gradient background
        // Create log panel (right side) with custom gradient background
        JPanel logPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(
                        0, 0, LOG_GRADIENT_START,
                        getWidth(), getHeight(), LOG_GRADIENT_END
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

// Configure text area
        logTextArea = new JTextArea(10, 30);
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Arial", Font.PLAIN, 14));
        logTextArea.setForeground(Color.BLACK);
        logTextArea.setOpaque(false);
        logTextArea.setBackground(new Color(0, 0, 0, 0)); // Fully transparent

// Configure scroll pane
        logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setOpaque(false);
        logScrollPane.getViewport().setOpaque(false);
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 255)),
                "Algorithm Log",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                new Color(0, 0, 255) // Light blue text color
        ));

        logPanel.add(logScrollPane, BorderLayout.CENTER);

        // Use JSplitPane to divide left and right panels
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, logPanel);
        splitPane.setResizeWeight(0.85); // Give even more space to the graph (changed from 0.7)
        splitPane.setDividerLocation(700); // Initial divider position (changed from 700)
        splitPane.setDividerSize(5); // Make the divider a bit thicker for better visibility

        // Add the split pane to the main content panel
        mainContentPanel.add(splitPane, BorderLayout.CENTER);

        // Add container to main frame
        mainFrame.add(mainContentPanel, BorderLayout.CENTER);

        // Set up node selection
        setupNodeSelection();

        // Initial log message
        logMessage("[System] Graph visualization initialized with " + adjacencyMatrix.length + " nodes");

        // Check if graph is disconnected
        if (isGraphDisconnected()) {
            logMessage("[Warning] Graph is disconnected - some nodes may not be reachable");
        }

        // Refresh the frame
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private static void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logTextArea.append(message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }

    private static void highlightSelectedNodes() {
        // Reset all nodes to default color first
        graph.nodes().forEach(node -> {
            node.removeAttribute("ui.class");
            node.setAttribute("ui.style", "fill-color: lightblue;");
        });

        // Highlight source node in green
        if (selectedSource != null && graph.getNode(selectedSource) != null) {
            graph.getNode(selectedSource).setAttribute("ui.class", "source");
        }

        // Highlight destination node in red
        if (selectedDestination != null && graph.getNode(selectedDestination) != null) {
            graph.getNode(selectedDestination).setAttribute("ui.class", "destination");
        }
    }

    private static void addNewNode() {
        // Create new node ID
        int newNodeIndex = adjacencyMatrix.length;
        String newNodeId = "N" + newNodeIndex;

        // Expand adjacency matrix
        int[][] newMatrix = new int[newNodeIndex + 1][newNodeIndex + 1];
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            System.arraycopy(adjacencyMatrix[i], 0, newMatrix[i], 0, adjacencyMatrix[i].length);
        }
        adjacencyMatrix = newMatrix;

        // Add node to graph
        graph.addNode(newNodeId);
        graph.getNode(newNodeId).setAttribute("ui.label", newNodeId);
        graph.getNode(newNodeId).setAttribute("ui.style", "fill-color: lightblue;");

        logMessage("[Modification] Added new node " + newNodeId);

        // Update UI
        populateNodeComboBoxes();
        setupNodeSelection();

        // Check if graph is now disconnected
        if (isGraphDisconnected()) {
            logMessage("[Warning] Graph is disconnected - some nodes may not be reachable");
        }
    }

    private static void removeSelectedNode() {
        if (selectedSource == null && selectedDestination == null) {
            JOptionPane.showMessageDialog(mainFrame, "Please select a node first by clicking on it");
            return;
        }

        String nodeToRemove = selectedSource != null ? selectedSource : selectedDestination;
        int nodeIndex = Integer.parseInt(nodeToRemove.substring(1));

        // Confirm removal
        int confirm = JOptionPane.showConfirmDialog(mainFrame,
                "Remove node " + nodeToRemove + " and all its connections?",
                "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Remove from graph
        graph.removeNode(nodeToRemove);

        // Update adjacency matrix
        int newSize = adjacencyMatrix.length - 1;
        int[][] newMatrix = new int[newSize][newSize];

        for (int i = 0, newI = 0; i < adjacencyMatrix.length; i++) {
            if (i == nodeIndex) continue;

            for (int j = 0, newJ = 0; j < adjacencyMatrix[i].length; j++) {
                if (j == nodeIndex) continue;
                newMatrix[newI][newJ] = adjacencyMatrix[i][j];
                newJ++;
            }
            newI++;
        }

        adjacencyMatrix = newMatrix;

        logMessage("[Modification] Removed node " + nodeToRemove + " and all its connections");

        // Reset selections
        selectedSource = null;
        selectedDestination = null;

        // Update UI
        populateNodeComboBoxes();
        setupNodeSelection();

        // Check if graph is now disconnected
        if (isGraphDisconnected()) {
            logMessage("[Warning] Graph is disconnected - some nodes may not be reachable");
        }
    }

    private static void addNewEdge() {
        if (selectedSource == null || selectedDestination == null) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Please select both source and destination nodes first");
            return;
        }

        if (selectedSource.equals(selectedDestination)) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Cannot create edge from a node to itself");
            return;
        }

        int weight = (Integer) edgeWeightSpinner.getValue();

        // Check for negative weights
        if (weight < 0) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Negative edge weights are not supported",
                    "Invalid Weight", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int sourceIndex = Integer.parseInt(selectedSource.substring(1));
        int destIndex = Integer.parseInt(selectedDestination.substring(1));

        // Check if edge already exists
        if (adjacencyMatrix[sourceIndex][destIndex] > 0) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Edge already exists between these nodes");
            return;
        }

        // Add edge to adjacency matrix
        adjacencyMatrix[sourceIndex][destIndex] = weight;
        adjacencyMatrix[destIndex][sourceIndex] = weight;

        // Add edge to graph
        String edgeId = sourceIndex < destIndex ?
                "E" + sourceIndex + "-" + destIndex :
                "E" + destIndex + "-" + sourceIndex;

        if (graph.getEdge(edgeId) == null) {
            graph.addEdge(edgeId, selectedSource, selectedDestination)
                    .setAttribute("ui.label", weight);
            logMessage("[Modification] Added edge " + edgeId + " between " +
                    selectedSource + " and " + selectedDestination + " with weight " + weight);
        }
    }

    private static void removeSelectedEdge() {
        if (selectedSource == null || selectedDestination == null) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Please select both source and destination nodes of the edge to remove");
            return;
        }

        int sourceIndex = Integer.parseInt(selectedSource.substring(1));
        int destIndex = Integer.parseInt(selectedDestination.substring(1));

        // Check if edge exists
        if (adjacencyMatrix[sourceIndex][destIndex] == 0) {
            JOptionPane.showMessageDialog(mainFrame,
                    "No edge exists between these nodes");
            return;
        }

        // Remove edge from adjacency matrix
        adjacencyMatrix[sourceIndex][destIndex] = 0;
        adjacencyMatrix[destIndex][sourceIndex] = 0;

        // Remove edge from graph
        String edgeId = sourceIndex < destIndex ?
                "E" + sourceIndex + "-" + destIndex :
                "E" + destIndex + "-" + sourceIndex;

        if (graph.getEdge(edgeId) != null) {
            graph.removeEdge(edgeId);
            logMessage("[Modification] Removed edge " + edgeId + " between " +
                    selectedSource + " and " + selectedDestination);
        }

        // Check if graph is now disconnected
        if (isGraphDisconnected()) {
            logMessage("[Warning] Graph is disconnected - some nodes may not be reachable");
        }
    }

    private static void populateNodeComboBoxes() {
        if (sourceComboBox == null || destinationComboBox == null) {
            return; // Combo boxes not initialized yet
        }

        sourceComboBox.removeAllItems();
        destinationComboBox.removeAllItems();

        for (int i = 0; i < adjacencyMatrix.length; i++) {
            String nodeId = "N" + i;
            sourceComboBox.addItem(nodeId);
            destinationComboBox.addItem(nodeId);
        }
    }

    private static class DFSStep {
        String nodeId;
        String edgeId;
        String type; // "visit", "consider", "backtrack"
        boolean allNodesVisited; // Flag to indicate if all nodes were visited at this step

        public DFSStep(String nodeId, String edgeId, String type) {
            this.nodeId = nodeId;
            this.edgeId = edgeId;
            this.type = type;
            this.allNodesVisited = false;
        }

        public DFSStep(String nodeId, String edgeId, String type, boolean allNodesVisited) {
            this.nodeId = nodeId;
            this.edgeId = edgeId;
            this.type = type;
            this.allNodesVisited = allNodesVisited;
        }
    }

    private static void startDFSTraversal() {
        if (isVisualizingDFS) return;

        if (selectedSource == null || selectedDestination == null) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Please select both source and destination nodes first!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if graph is disconnected
        if (isGraphDisconnected()) {
            logMessage("[Warning] Graph is disconnected - some nodes may not be reachable");
        }

        viewer.disableAutoLayout();
        isVisualizingDFS = true;
        dfsTraversalOrder = new ArrayList<>();
        currentDfsStep = 0;

        // Reset all nodes and edges
        resetGraphColors();

        // Highlight source and destination nodes
        graph.getNode(selectedSource).setAttribute("ui.class", "source");
        graph.getNode(selectedDestination).setAttribute("ui.class", "destination");

        logMessage("\n[Algorithm] Starting DFS traversal from " + selectedSource + " to " + selectedDestination);
        logMessage("[Algorithm] Step-by-step traversal:");

        // Perform DFS to collect traversal steps
        performDFS(selectedSource, new HashSet<>(), null);

        dfsTimer = new Timer(visualizationSpeed, e -> {
            if (currentDfsStep < dfsTraversalOrder.size()) {
                DFSStep step = dfsTraversalOrder.get(currentDfsStep);

                // Reset previous highlights
                if (currentDfsStep > 0) {
                    DFSStep prevStep = dfsTraversalOrder.get(currentDfsStep - 1);
                    if (prevStep.edgeId != null && graph.getEdge(prevStep.edgeId) != null) {
                        graph.getEdge(prevStep.edgeId).removeAttribute("ui.class");
                    }
                }

                // Check if all nodes have been visited
                if (step.allNodesVisited) {
                    dfsTimer.stop();
                    isVisualizingDFS = false;
                    viewer.enableAutoLayout();
                    logMessage("[Algorithm] All nodes visited - proceeding to find shortest path");
                    highlightShortestPath();
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    return;
                }

                // Apply current step highlights and log
                switch (step.type) {
                    case "visit":
                        graph.getNode(step.nodeId).setAttribute("ui.class", "current");
                        logMessage("[Step " + (currentDfsStep + 1) + "] Visiting node: " + step.nodeId);
                        break;
                    case "consider":
                        if (step.edgeId != null && graph.getEdge(step.edgeId) != null) {
                            graph.getEdge(step.edgeId).setAttribute("ui.class", "considered");
                        }
                        break;
                    case "backtrack":
                        if (step.edgeId != null && graph.getEdge(step.edgeId) != null) {
                            graph.getEdge(step.edgeId).setAttribute("ui.class", "backtrack");
                        }
                        break;
                }

                currentDfsStep++;

                // Check if we've completed the traversal
                if (currentDfsStep == dfsTraversalOrder.size()) {
                    dfsTimer.stop();
                    isVisualizingDFS = false;
                    viewer.enableAutoLayout();
                    logMessage("[Algorithm] DFS traversal completed");
                    highlightShortestPath();
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                }
            } else {
                dfsTimer.stop();
                isVisualizingDFS = false;
                viewer.enableAutoLayout();
                logMessage("[Algorithm] DFS traversal completed");
                highlightShortestPath();
                pauseButton.setEnabled(false);
                resumeButton.setEnabled(false);
            }
        });

        dfsTimer.start();
    }

    private static void highlightShortestPath() {
        logMessage("\n[Pathfinding] Starting Dijkstra's algorithm to find shortest path");
        logMessage("[Pathfinding] Source: " + selectedSource + ", Destination: " + selectedDestination);

        // Dijkstra's algorithm to find shortest path
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>();
        Set<String> visited = new HashSet<>();

        // Initialize distances
        graph.nodes().forEach(node -> distances.put(node.getId(), Integer.MAX_VALUE));
        distances.put(selectedSource, 0);
        queue.add(new NodeDistance(selectedSource, 0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            String u = current.nodeId;

            if (visited.contains(u)) continue;
            visited.add(u);

            if (u.equals(selectedDestination)) {
                break;
            }

            int uIndex = Integer.parseInt(u.substring(1));
            for (int vIndex = 0; vIndex < adjacencyMatrix.length; vIndex++) {
                if (adjacencyMatrix[uIndex][vIndex] > 0) {
                    String v = "N" + vIndex;
                    int alt = distances.get(u) + adjacencyMatrix[uIndex][vIndex];
                    if (alt < distances.get(v)) {
                        distances.put(v, alt);
                        previous.put(v, u);
                        queue.add(new NodeDistance(v, alt));
                    }
                }
            }
        }

        // Handle case where destination is unreachable
        if (!previous.containsKey(selectedDestination)) {
            logMessage("[Pathfinding] Destination node is unreachable from source");
            JOptionPane.showMessageDialog(mainFrame,
                    "Destination node is unreachable from source!",
                    "Path Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Highlight the shortest path
        List<String> path = new ArrayList<>();
        for (String node = selectedDestination; node != null; node = previous.get(node)) {
            path.add(node);
        }
        Collections.reverse(path);

        logMessage("[Pathfinding] Path found! Total distance: " + distances.get(selectedDestination));
        logMessage("[Pathfinding] Path: " + String.join(" -> ", path));

        // Use an array to hold the current step (effectively final for the inner class)
        final int[] currentStep = {0};

        // Create a timer to animate the path highlighting
        Timer pathTimer = new Timer(visualizationSpeed / 2, e -> {
            if (currentStep[0] < path.size()) {
                String nodeId = path.get(currentStep[0]);
                graph.getNode(nodeId).setAttribute("ui.class", "path");

                if (currentStep[0] > 0) {
                    String prevNodeId = path.get(currentStep[0] - 1);
                    int a = Integer.parseInt(prevNodeId.substring(1));
                    int b = Integer.parseInt(nodeId.substring(1));
                    String edgeId = a < b ? "E" + a + "-" + b : "E" + b + "-" + a;
                    if (graph.getEdge(edgeId) != null) {
                        graph.getEdge(edgeId).setAttribute("ui.class", "path");
                    }
                }
                currentStep[0]++;
            } else {
                ((Timer) e.getSource()).stop();
                JOptionPane.showMessageDialog(mainFrame,
                        "Shortest path distance: " + distances.get(selectedDestination),
                        "Path Found", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        pathTimer.start();
    }

    private static boolean performDFS(String nodeId, Set<String> visited, String incomingEdgeId) {
        if (nodeId == null) {
            return false;
        }

        // Mark node as visited if not already visited
        boolean isNewVisit = !visited.contains(nodeId);
        if (isNewVisit) {
            visited.add(nodeId);
            dfsTraversalOrder.add(new DFSStep(nodeId, incomingEdgeId, "visit"));

            // Check if all nodes have been visited
            if (visited.size() == adjacencyMatrix.length) {
                dfsTraversalOrder.add(new DFSStep(nodeId, null, "visit", true));
                logMessage("[Completion] All nodes have been visited");
            }
        } else if (incomingEdgeId != null) {
            // If we're revisiting via an edge, add a backtrack step
            dfsTraversalOrder.add(new DFSStep(nodeId, incomingEdgeId, "backtrack"));
        }

        // Check if we've reached the destination
        boolean foundDestination = nodeId.equals(selectedDestination);

        int nodeIndex = Integer.parseInt(nodeId.substring(1));

        // Visit all neighbors
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            if (adjacencyMatrix[nodeIndex][i] > 0) {
                String neighborId = "N" + i;
                String edgeId = nodeIndex < i ? "E" + nodeIndex + "-" + i : "E" + i + "-" + nodeIndex;

                // Add step to show edge being considered
                dfsTraversalOrder.add(new DFSStep(nodeId, edgeId, "consider"));

                if (!visited.contains(neighborId)) {
                    foundDestination = performDFS(neighborId, visited, edgeId) || foundDestination;
                    if (foundDestination && visited.size() == adjacencyMatrix.length) {
                        return true; // Early exit if we've found destination and visited all nodes
                    }
                }
            }
        }

        return foundDestination;
    }

    private static void setupNodeSelection() {
        graph.nodes().forEach(node -> {
            node.removeAttribute("ui.click");
        });

        graph.nodes().forEach(node -> {
            node.setAttribute("ui.click", (Runnable) () -> {
                handleNodeSelection(node, node.getId());
            });
        });
    }

    private static void handleNodeSelection(org.graphstream.graph.Node node, String nodeId) {
        // Just update the combo boxes without changing colors
        if (selectedSource == null) {
            selectedSource = nodeId;
            sourceComboBox.setSelectedItem(nodeId);
            logMessage("[Selection] Source node set to " + nodeId);
        }
        else if (selectedDestination == null && !nodeId.equals(selectedSource)) {
            selectedDestination = nodeId;
            destinationComboBox.setSelectedItem(nodeId);
            logMessage("[Selection] Destination node set to " + nodeId);
        }
        else {
            if (nodeId.equals(selectedSource)) {
                selectedSource = null;
                sourceComboBox.setSelectedItem(null);
                logMessage("[Selection] Source node cleared");
            } else if (nodeId.equals(selectedDestination)) {
                selectedDestination = null;
                destinationComboBox.setSelectedItem(null);
                logMessage("[Selection] Destination node cleared");
            }
        }
    }

    private static class NodeDistance implements Comparable<NodeDistance> {
        String nodeId;
        int distance;

        public NodeDistance(String nodeId, int distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    private static void resetGraphColors() {
        if (dfsTimer != null && dfsTimer.isRunning()) {
            dfsTimer.stop();
        }
        isVisualizingDFS = false;

        if (viewer != null) {
            viewer.disableAutoLayout();
        }

        graph.nodes().forEach(node -> {
            node.removeAttribute("ui.class");
            node.setAttribute("ui.style", "fill-color: lightblue;");
        });

        graph.edges().forEach(edge -> {
            edge.removeAttribute("ui.class");
            edge.setAttribute("ui.style", "fill-color: #777; size: 2px;");
        });

        setupNodeSelection();

        if (viewer != null) {
            viewer.enableAutoLayout();
        }
    }

    private static String getStylesheet() {
        return """
        node {
            size: 30px;
            fill-color: lightblue;
            stroke-mode: plain;
            stroke-color: #333;
            stroke-width: 2px;
            text-size: 20;
            text-color: black;
        }
        node.source {
            fill-color: green;
            size: 35px;
        }
        node.destination {
            fill-color: red;
            size: 35px;
        }
        node.visited {
            fill-color: #1F598C;
            size: 30px;
        }
        node.path {
            fill-color: purple;
            size: 35px;
        }
        node.current {
            fill-color: yellow;
            size: 40px;
        }
        edge {
            size: 2px;
            fill-color: #777;
            arrow-size: 10px, 4px;
        }
        edge.considered {
            fill-color: red;
            size: 4px;
        }
        edge.backtrack {
            fill-color: gray;
            size: 2px;
            arrow-shape: none;
        }
        edge.path {
            fill-color: blue;
            size: 4px;
        }
        """;
    }
}