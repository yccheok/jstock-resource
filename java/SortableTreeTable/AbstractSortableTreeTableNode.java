/** AbstractSortableTreeTableNode.java                          PortfolioApp
 *
 * Created 23/12/2007 3:19:56 PM
 * 
 * @author Ray Turnbull
 */
package org.codelutin.jtimer.ui.treetable.sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.tree.TreeNode;

import org.jdesktop.swingx.decorator.SortOrder;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

/**
 * This is designed to be used with SortableTreeTableModel.
 * SortableTreeTable Model extends DefaultTreeTableModel.<br>
 * For user interaction by clicking column headers, SortableTreeTable (extends
 * JXTreeTable should be used also.
 * <p>
 * It sorts the children of this node.<br><br>
 * Note that this does not support sorting the underlying table, as that is
 * meaningless for a TreeTable. 
 * If the sort column contains values from multiple levels of the table, only
 * those rows which are children of this node will be sorted.<br>
 * Obviously, if a column contains values from children of multiple nodes, 
 * and all nodes are sortable, the children of each node will be sorted 
 * separately.
 * <p>
 * After the root of this node has been attached to a model, all additions or
 * deletions of children should be done through the model (which is standard 
 * for DefaultTreeTableModel)
 * <p>
 * This class does not handle changes to the values of fields being sorted. 
 * This must be done manually by calling
 * {@link SortableTreeTableModel#sort(TreeTableNode)} or
 * {@link SortableTreeTableModel#sort()} to re-sort the whole Treetable
 * 
 */
public abstract class AbstractSortableTreeTableNode extends
		AbstractMutableTreeTableNode {
	
	private boolean sortable = true;
	private boolean sorted = false;
	private int[] modelToView;
	private Row[] viewToModel;
	
	/**
	 * 
	 */
	public AbstractSortableTreeTableNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param userObject
	 */
	public AbstractSortableTreeTableNode(Object userObject) {
		super(userObject);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param userObject
	 * @param allowsChildren
	 */
	public AbstractSortableTreeTableNode(Object userObject,
			boolean allowsChildren) {
		super(userObject, allowsChildren);
	}
	
	public AbstractSortableTreeTableNode(boolean sortable, Object userObject) {
		super(userObject);
		this.sortable = sortable;
	}
	
	// ==================================================== overridden methods
	
	@Override
	public TreeTableNode getChildAt(int childIndex) {
		if (!sortable || !sorted) {
			return super.getChildAt(childIndex);
		}
		return super.getChildAt(convertRowIndexToModel(childIndex));
	}
	
	@Override
	public int getIndex(TreeNode node) {
		int x = children.indexOf(node);
		return(convertRowIndexToView(x));
	}
	
	// ======================================================= public methods

    public int convertRowIndexToModel(int index) {
        if (sorted) {
        	return viewToModel[index].modelIndex;
		} else {
			return index;
		}
    }

    public int convertRowIndexToView(int index) {
        if (sorted) {
        	if (index == -1 || index >= modelToView.length) {
        		return index;
        	} else {
        		return modelToView[index];
        	}
        } else {
        	return index;
		}
    }

    /**
     * This should only be called before nodes are added to model. It does not
     * instigate any action. After model set up use 
     * {@link SortableTreeTableModel#setSortable(MutableTreeTableNode, boolean)}
     * @param sortable 
     */
    public void setSortable(boolean sortable) {
    	this.sortable = sortable;
    }
    
    /**
     * This is a utility method to presort children<br>
     * It should only be used before the nodes are attached to a model. Of 
     * itself, it will not cause the tree to be rebuilt.<br>
     * If run on a sorted node, the sort should be run again. See {@link
     * SortableTreeTableModel#sort(TreeTableNode)}
     * 
     * @param column - the index of the column to sort on
     * @param ascending
     */
    public void presortChildren(int column, boolean ascending) {
		int count = getChildCount();
		if (count == 0) return;
		Row[] work = new Row[count];
		int x = 0;
		SortOrder order = 
					(ascending) ? SortOrder.ASCENDING : SortOrder.DESCENDING;
		for (TreeTableNode node : children) {
			Row r = new Row(node.getValueAt(column), x, order);
			work[x++] = r;			
		}
		// sort
		Arrays.sort(work);
		// rebuild children
		List<MutableTreeTableNode> newChildren = 
						new ArrayList<MutableTreeTableNode>(count);

		for (Row r : work) {
			MutableTreeTableNode node = children.get(r.modelIndex);
			newChildren.add(node);
		}
		for (int i = 0; i < newChildren.size(); i++) {
			children.set(i, newChildren.get(i));			
		}
	}

    // ===================================================== protected methods
    
	/**
	 * subclasses should override if applicable, e.g. if particular column
	 * does not exist in this nodes children
	 */
	protected boolean canSort(String column) {
		return true;
	}
	
	// ======================================================= package methods
	// following only called from SortableTreeTableModel
	
	void sort(int column, SortOrder order) {
		int count = getChildCount();
		if (count == 0) {
			sorted = false;
			return;
		}
		modelToView = new int[count];
		// load view to model array
		viewToModel = new Row[count];
		int x = 0;
		for (TreeTableNode node : children) {
			Row r = new Row(node.getValueAt(column), x, order);
			viewToModel[x++] = r;			
		}
		// sort
		Arrays.sort(viewToModel);
		// load model to view array
        for (int i = count - 1; i >= 0; i--) {
            modelToView[viewToModel[i].modelIndex] = i;
        }
        sorted = true;
	}
	
	void reset() {
		sorted = false;
	}
	
	boolean isSorted() {
		return sorted;
	}
	
	boolean canSort() { 
		if (!sortable || !getAllowsChildren()) return false;
		int count = getChildCount();
		if (count == 0) {
			return false;
		}
		return true;
	}
	
	// ======================================================= private methods
	

	private class Row implements Comparable<Row> {
		@SuppressWarnings("unchecked")
		Comparable key;
		int modelIndex;
		SortOrder order;
		
		@SuppressWarnings("unchecked")
		public Row(Object key, int modelIndex, SortOrder order) {
			this.key = (Comparable)key;
			this.modelIndex = modelIndex;
			this.order = order;
		}
		
		@SuppressWarnings("unchecked")
		public int compareTo(Row r) {
			int result;
			// treat null as less than non-null
			if (key == null) {
				if (r.key == null) {
					result = 0;
				} else {
					result = -1;
				}
			} else {
				if (r.key == null) {
					result = 1;
				} else {
					result = key.compareTo(r.key);
				}
			}
			if (!order.isAscending()) {
				result *= -1;
			}
			if (result == 0) {
				// revert to model order
				result = modelIndex - r.modelIndex;
			}
			return result;
		}
		
	}
}
