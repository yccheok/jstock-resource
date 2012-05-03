/** SortableTreeTableDemo.java                          
 *
 * Created 10/01/2008 
 * 
 * @author Ray Turnbull
 */
package org.codelutin.jtimer.ui.treetable.sorting;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.VerticalLayout;

/**
 *
 */
public class SortableTreeTableDemo {

    private final String[] headings = { "Family Name", "Given Name", "Post Code",
            "Relationship", "DOB", "Sex" };
    private enum EditAction {ADD, MODIFY, DELETE, NO_ACTION};
    private EditAction action = EditAction.NO_ACTION;
    private Node selectedNode;
    private JButton add;
    private JButton modify;
    private JButton delete;
    private Node root;
    private SortableTreeTableModel model;
    private TreeSelectionModel selector;
    private JXTreeTable table;
    private Object[][] initialData;
    private Object[][] childData;
    private DateFormat format;
    
    private JXCollapsiblePane cp;
    private JTextField familyNameC;
    private JTextField givenNameC;
    private JTextField postCodeC;
    private JTextField relationshipC;
    private JXDatePicker dobC;
    private JTextField sexC;
    private JButton okButton;
    private JComponent focus;

    public void show() {
        JFrame f = new JFrame("Sortable TreeTable Demo");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setMinimumSize(new Dimension(700, 600));
        f.setLocationRelativeTo(null);
        f.setContentPane(getPanel());
        f.pack();
        f.setVisible(true);
    }
    
    private JPanel getPanel() {
        JPanel p = new JPanel(new VerticalLayout());
        JScrollPane sp = new JScrollPane(getTreeTable());
        p.add(sp);
        p.add(getButtons());
        p.add(getDetailPanel());
        return p;
    }
    
    // ============================================================ processing
    
    private void doAction(EditAction a) {
        action = a;
        TreePath path = selector.getSelectionPath();
        if (path == null) return;
        disableButtons();
        selectedNode = (Node) path.getLastPathComponent();
        if (action == EditAction.ADD)  {            
            // for add processing child of selected node
            clearFields();
            enableFields(true);
            if (selectedNode instanceof PersonNode) {
                familyNameC.setText(selectedNode.getKey());
                familyNameC.setEditable(false);
                familyNameC.setFocusable(false);
                postCodeC.setEnabled(false);
                focus = givenNameC;
            } else {        // root
                relationshipC.setEnabled(false);
                focus = familyNameC;
            }
        }
        if (action == EditAction.DELETE) {
            loadFields(selectedNode);
            enableFields(false);
            focus = okButton;
        }
        if (action == EditAction.MODIFY) {
            enableFields(true);
            loadFields(selectedNode);
            if (selectedNode instanceof DependantNode) {
                familyNameC.setEditable(false);
                familyNameC.setFocusable(false);
                postCodeC.setEnabled(false);
                focus = givenNameC;
            } else {        // person
                relationshipC.setEnabled(false);
                focus = familyNameC;
            }
        }
        cp.setCollapsed(false);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                focus.requestFocusInWindow();
            }
            
        });
    }
    
    private void enableFields(boolean enable) {
        familyNameC.setEnabled(enable);
        givenNameC.setEnabled(enable);
        postCodeC.setEnabled(enable);
        relationshipC.setEnabled(enable);
        dobC.setEnabled(enable);
        sexC.setEnabled(enable);
    }
    
    private void clearFields() {
        familyNameC.setText("");
        givenNameC.setText("");
        postCodeC.setText("");
        relationshipC.setText("");
        dobC.setDate(null);
        sexC.setText("");
    }
    
    private void loadFields(Node node) {
        clearFields();
        if (node instanceof PersonNode) {
            familyNameC.setText(node.getKey());
        } else {
            familyNameC.setText(((Node)node.getParent()).getKey());         
        }
        Object[] data = (Object[]) node.getUserObject();
        givenNameC.setText((String)data[1]);
        if (node instanceof PersonNode) {
            postCodeC.setText(((Integer)data[2]).toString());
        }
        if (node instanceof DependantNode) {
            relationshipC.setText((String) data[3]);
        }
        dobC.setDate((Date)data[4]);
        sexC.setText((String)data[5]);
    }
    
    private void enableButtons(Node node) {
        if (node == null) {
            add.setEnabled(false);
            modify.setEnabled(false);
            delete.setEnabled(false);
            return;
        }
        if (action != EditAction.NO_ACTION) return;
        if (node instanceof RootNode) {
            add.setEnabled(true);
            modify.setEnabled(false);
            delete.setEnabled(false);           
        } else if (node instanceof PersonNode) {
            add.setEnabled(true);
            modify.setEnabled(true);
            delete.setEnabled(true);
            if (node.getChildCount() > 0) {
                delete.setEnabled(false);
            }           
        } else {
            add.setEnabled(false);
            modify.setEnabled(true);
            delete.setEnabled(true);            
        }       
    }
    
    private void disableButtons() {
        add.setEnabled(false);
        modify.setEnabled(false);
        delete.setEnabled(false);
    }
    
    private void finish(boolean option) {
        if (option == false) {
            cleanUp();
        } else if (action == EditAction.DELETE) {
            model.removeNodeFromParent(selectedNode);
            cleanUp();
        } else if (action == EditAction.ADD) {
            Object[] data = buildData();
            Node child;
            if (selectedNode instanceof RootNode) {
                child = new PersonNode(data);
            } else {
                child = new DependantNode(data);
            }
            model.insertNodeInto(child, selectedNode);
            cleanUp();
        } else if (action == EditAction.MODIFY) {
            Object[] data = buildData();
            selectedNode.setUserObject(data);
            cleanUp();
            // must do manual sort as system does not track data changes
            model.sort(selectedNode.getParent());
        }
        
    }
    
    private Object[] buildData() {
        Object[] data = new Object[6];
        data[1] = givenNameC.getText();
        if ((selectedNode instanceof RootNode) ||
            (selectedNode instanceof PersonNode && action == EditAction.MODIFY)) {
            data[0] = familyNameC.getText();
            data[2] = new Integer(postCodeC.getText());
            data[3] = null;
        } else {
            data[0] = "";
            data[2] = null;
            data[3] = relationshipC.getText();
        }
        data[4] = dobC.getDate();
        data[5] = sexC.getText();
        return data;
    }
    
    private void cleanUp() {
        cp.setCollapsed(true);
        familyNameC.setEditable(true);
        familyNameC.setFocusable(true);
        action = EditAction.NO_ACTION;
        TreePath path = selector.getSelectionPath();
        Node node;
        if (path == null) {
            node = null;
        } else {
            node = (Node) path.getLastPathComponent();
        }
        enableButtons(node);
    }
    
    // ============================================================= treetable
    
    private JXTreeTable getTreeTable() {
        loadData();
        root = new RootNode("People");
        addRootChildren();
        model = new SortableTreeTableModel(root, Arrays.asList(headings)) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 2) {
                    return Integer.class;
                } else if (column == 4) {
                    return Date.class;
                } else {
                    return String.class;
                }
            }
        };
        table = new SortableTreeTable(model);
        table.setShowGrid(true, true);
        table.setRootVisible(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setColumnControlVisible(true);
        table.setHorizontalScrollEnabled(true);
        table.setFillsViewportHeight(false);

        // this must be before any sort instructions or get funny results
        table.setAutoCreateColumnsFromModel(false);
        
        selector = table.getTreeSelectionModel();
        selector.addTreeSelectionListener(new Listener());
        table.expandRow(0);
        table.packAll();
        //model.setSortColumn("Family Name");
        return table;
    }

    private void addRootChildren() {
        for (Object[] element : initialData) {
            Node child = new PersonNode(element);
            root.add(child);
            addChildren(child);
            child.presortChildren(4, true);
        }
    }
    
    private void addChildren(Node parent) {
        String key = parent.getKey();
        for (Object[] element : childData) {
            String key2 = element[0].toString();
            if (key2.equals(key)) {
                Object[] newElement = Arrays.copyOf(element, element.length);
                Node child = new DependantNode(newElement);
                parent.add(child);
            }
        }
    }
    
    private void loadData() {
        format = new SimpleDateFormat("dd/MM/yyyy");
        try {
            initialData = new Object[][] {
            {"Turnbull", "Ray", 2196, null, format.parse("01/01/1900"), "M"},
            {"Hadden", "Brian", 2030, null, format.parse("15/07/1950"), "M"},
            {"Lesnie", "James", 2587, null, format.parse("27/03/1976"), "M"},
            {"Neyle", "Barbara", 2258, null, format.parse("26/08/1975"), "F"},
            {"Dowd", "Charles", 2658, null, format.parse("12/07/1982"), "M"},
            {"Powers", "Raymond", 2272, null, format.parse("26/02/1940"), "M"},
            {"Maguire", "Jerry", 2050, null, format.parse("30/11/1953"), "M"},
            {"Lee", "Stan", 2666, null, format.parse("01/10/1981"), "M"}
            };
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try {
            childData = new Object[][] {
            {"Hadden", "James", null, "Son", format.parse("15/07/1975"), "M"},
            {"Hadden", "Lois", null, "Daughter", format.parse("15/07/1977"), "F"},
            {"Hadden", "Lucy", null, "Daughter", format.parse("15/07/1978"), "F"},
            {"Hadden", "Margaret", null, "Wife", format.parse("15/07/1953"), "F"},
            {"Lesnie", "Clare", null, "Wife", format.parse("27/03/1976"), "F"},
            {"Lesnie", "Maya", null, "Daughter", format.parse("27/03/1987"), "F"},
            {"Neyle", "Robert", null, "Husband", format.parse("26/08/1973"), "M"},
            {"Neyle", "Clarise", null, "Daughter", format.parse("26/08/2000"), "F"},
            {"Neyle", "Robert Jnr", null, "Son", format.parse("26/08/1998"), "M"},
            };
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    // ========================================================== button panel
    
    private JPanel getButtons() {
        buildButtons();
        JPanel p = new JPanel();
        p.add(add);
        p.add(modify);
        p.add(delete);
        return p;
    }

    private void buildButtons() {
        add = new JButton("Add");
        add.setEnabled(false);
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doAction(EditAction.ADD);
            }
        }); 
        modify = new JButton("Modify");
        modify.setEnabled(false);
        modify.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doAction(EditAction.MODIFY);                
            }
        }); 
        delete = new JButton("Delete");
        delete.setEnabled(false);
        delete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doAction(EditAction.DELETE);                
            }
        }); 
    }
    
    // ========================================================== detail panel
    
    private JXCollapsiblePane getDetailPanel() {
        cp = new JXCollapsiblePane();
        cp.setCollapsed(true);
        familyNameC = new JTextField(20);
        givenNameC = new JTextField(20);
        postCodeC = new JTextField(10);
        relationshipC = new JTextField(10);
        dobC = new JXDatePicker();
        sexC = new JTextField(2);
        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                finish(true);               
            }
        }); 
        JButton b2 = new JButton("Cancel");
        b2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                finish(false);              
            }
        }); 
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 10, 2, 5);
        p.add(new JLabel("Family Name"), c);
        p.add(familyNameC, c);
        p.add(new JLabel("Given Name"), c);
        p.add(givenNameC, c);
        c.gridy = 1;
        p.add(new JLabel("Post Code"), c);
        p.add(postCodeC, c);
        p.add(new JLabel("RelationShip"), c);
        p.add(relationshipC, c);
        c.gridy = 2;
        p.add(new JLabel("Date of Birth"), c);
        p.add(dobC, c);
        p.add(new JLabel("Sex"), c);
        p.add(sexC, c);
        c.gridy = 3;
        c.gridx = 1;
        c.anchor = GridBagConstraints.EAST;
        p.add(okButton, c);
        c.gridx = 2;
        c.anchor = GridBagConstraints.WEST;
        p.add(b2, c);
        cp.setContentPane(p);
        return cp;
    }
    
    // ======================================== tree selection listener class
    
    private class Listener implements TreeSelectionListener {

        /* 
         * Inherited
         */
        @Override
        public void valueChanged(TreeSelectionEvent arg0) {
            Node node;
            if (arg0.getNewLeadSelectionPath() == null) {
                node = null;
            } else {
                TreePath path = arg0.getPath();
                if (path == null) {
                    node = null;
                } else {
                    node = (Node) path.getLastPathComponent();
                }
            }
            enableButtons(node);
        }
        
    }
    
    // ========================================================== node classes
    
    private class Node extends AbstractSortableTreeTableNode {
        
        protected String key;
        
        public Node(Object[] data) {
            super(data);
            if (data == null) {
                throw new IllegalArgumentException("Node data cannot be null");
            }
            key = data[0].toString();
        }
        
        public String getKey() {
            return key;
        }
        
        /* 
         * Inherited
         */
        @Override
        public int getColumnCount() {
            Object[] data = (Object[]) getUserObject();
            return data.length;
        }

        /* 
         * Inherited
         */
        @Override
        public Object getValueAt(int column) {
            Object[] data = (Object[]) getUserObject();
            return data[column];
        }
        
    }
    
    private class RootNode extends Node {
        
        public RootNode(String key) {
            super(new Object[] {key});
        }
    }
    
    private class PersonNode extends Node {
        
        public PersonNode(Object[] data) {
            super(data);
        }
        
    }
    
    private class DependantNode extends Node {
        
        public DependantNode(Object[] data) {
            super(data);
            // remove family name
            data[0] = null;
            key = "";
        }
    }
    
    // =================================================================== main
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        new SortableTreeTableDemo().show();
    }

}
