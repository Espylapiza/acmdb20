package simpledb;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Join operator implements the relational join operation.
 */
public class HashEquiJoin extends Operator {

    private static final long serialVersionUID = 1L;

    private final JoinPredicate p;
    private DbIterator child1;
    private DbIterator child2;

    private ConcurrentHashMap<Field, ArrayList<Tuple>> map1;

    private transient Iterator<Tuple> listIt1 = null;
    private Tuple t2;

    /**
     * Constructor. Accepts to children to join and the predicate to join them
     * on
    
     * @param p
     *            The predicate to use to join the children
     * @param child1
     *            Iterator for the left(outer) relation to join
     * @param child2
     *            Iterator for the right(inner) relation to join
     */
    public HashEquiJoin(JoinPredicate p, DbIterator child1, DbIterator child2) {
        // some code goes here
        this.p = p;
        this.child1 = child1;
        this.child2 = child2;
    }

    public JoinPredicate getJoinPredicate() {
        // some code goes here
        return this.p;
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        return TupleDesc.merge(this.child1.getTupleDesc(), this.child2.getTupleDesc());
    }

    public String getJoinField1Name() {
        // some code goes here
        return this.child1.getTupleDesc().getFieldName(this.p.getField1());
    }

    public String getJoinField2Name() {
        // some code goes here
        return this.child2.getTupleDesc().getFieldName(this.p.getField2());
    }

    public void open() throws DbException, NoSuchElementException, TransactionAbortedException {
        // some code goes here
        this.child1.open();
        this.child2.open();

        this.initMap();
        this.child1.close();

        super.open();
    }

    public void close() {
        // some code goes here
        this.child2.close();
        super.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        this.child1.rewind();
        this.child2.rewind();
    }

    /**
     * Returns the next tuple generated by the join, or null if there are no
     * more tuples. Logically, this is the next tuple in r1 cross r2 that
     * satisfies the join predicate. There are many possible implementations;
     * the simplest is a nested loops join.
     * <p>
     * Note that the tuples returned from this particular implementation of Join
     * are simply the concatenation of joining tuples from the left and right
     * relation. Therefore, there will be two copies of the join attribute in
     * the results. (Removing such duplicate columns can be done with an
     * additional projection operator if needed.)
     * <p>
     * For example, if one tuple is {1,2,3} and the other tuple is {1,5,6},
     * joined on equality of the first column, then this returns {1,2,3,1,5,6}.
     * 
     * @return The next matching tuple.
     * @see JoinPredicate#filter
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        if (listIt1 != null && listIt1.hasNext()) {
            return Tuple.merge(listIt1.next(), this.t2);
        }

        for (; this.child2.hasNext();) {
            this.t2 = this.child2.next();
            Field f2 = this.t2.getField(p.getField2());

            ArrayList<Tuple> arr1 = this.map1.get(f2);

            if (arr1 != null) {
                listIt1 = arr1.iterator();
                return Tuple.merge(listIt1.next(), this.t2);
            }
        }
        return null;
    }

    @Override
    public DbIterator[] getChildren() {
        // some code goes here
        return new DbIterator[] { this.child1, this.child2 };
    }

    @Override
    public void setChildren(DbIterator[] children) {
        // some code goes here
        this.child1 = children[0];
        this.child2 = children[1];
    }

    private void initMap() throws DbException, NoSuchElementException, TransactionAbortedException {
        this.map1 = new ConcurrentHashMap<Field, ArrayList<Tuple>>();
        for (; this.child1.hasNext();) {
            Tuple t1 = this.child1.next();
            Field f1 = t1.getField(p.getField1());

            ArrayList<Tuple> arr1 = this.map1.get(f1);

            if (arr1 == null) {
                arr1 = new ArrayList<Tuple>();
                this.map1.put(f1, arr1);
            }

            arr1.add(t1);
        }
    }

}
