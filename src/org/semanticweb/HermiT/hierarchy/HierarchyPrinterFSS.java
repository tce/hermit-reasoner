// Copyright 2008 by Oxford University; see license.txt for details
package org.semanticweb.HermiT.hierarchy;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Role;
import org.semanticweb.HermiT.model.InverseRole;

public class HierarchyPrinterFSS {
    protected final PrintWriter m_out;
    protected final String m_defaultPrefixURI;
    protected final Set<String> m_prefixURIs;
    protected Prefixes m_prefixes;
    
    public HierarchyPrinterFSS(PrintWriter out,String defaultPrefixURI) {
        m_out=out;
        m_defaultPrefixURI=defaultPrefixURI;
        m_prefixURIs=new TreeSet<String>();
        m_prefixURIs.add(defaultPrefixURI);
        m_prefixURIs.add(Prefixes.s_semanticWebPrefixes.get("owl"));
    }
    public void loadAtomicConceptPrefixURIs(Collection<AtomicConcept> atomicConcepts) {
        for (AtomicConcept atomicConcept : atomicConcepts) {
            String uri=atomicConcept.getURI();
            int hashIndex=uri.indexOf('#');
            if (hashIndex!=-1) {
                String prefixURI=uri.substring(0,hashIndex+1);
                String localName=uri.substring(hashIndex+1);
                if (Prefixes.isValidLocalName(localName))
                    m_prefixURIs.add(prefixURI);
            }
        }
    }
    public void loadAtomicRolePrefixURIs(Collection<AtomicRole> atomicRoles) {
        for (AtomicRole atomicRole : atomicRoles) {
            String uri=atomicRole.getURI();
            int hashIndex=uri.indexOf('#');
            if (hashIndex!=-1) {
                String prefixURI=uri.substring(0,hashIndex+1);
                String localName=uri.substring(hashIndex+1);
                if (Prefixes.isValidLocalName(localName))
                    m_prefixURIs.add(prefixURI);
            }
        }
    }
    public void startPrinting() {
        String owlPrefixURI=Prefixes.s_semanticWebPrefixes.get("owl");
        m_prefixes=new Prefixes();
        m_prefixes.declareDefaultPrefix(m_defaultPrefixURI);
        m_prefixes.declarePrefix("owl",owlPrefixURI);
        int index=1;
        for (String prefixURI : m_prefixURIs)
            if (!m_defaultPrefixURI.equals(prefixURI) && !owlPrefixURI.equals(prefixURI)) {
                String prefix="a"+(index++);
                m_prefixes.declarePrefix(prefix,prefixURI);
            }
        for (Map.Entry<String,String> entry : m_prefixes.getPrefixIRIsByPrefixName().entrySet())
            if (!"owl".equals(entry.getKey()))
                m_out.println("Prefix("+entry.getKey()+":=<"+entry.getValue()+">)");
        m_out.println();
        m_out.println("Ontology(<"+m_prefixes.getPrefixIRIsByPrefixName().get("")+">");
        m_out.println();
    }
    public void printAtomicConceptHierarchy(Hierarchy<AtomicConcept> atomicConceptHierarchy) {
        Hierarchy<AtomicConcept> sortedAtomicConceptHierarchy=atomicConceptHierarchy.transform(new IdentityTransformer<AtomicConcept>(),AtomicConceptComparator.INSTANCE);
        AtomicConceptPrinter atomicConceptPrinter=new AtomicConceptPrinter(sortedAtomicConceptHierarchy.getBottomNode());
        sortedAtomicConceptHierarchy.traverseDepthFirst(atomicConceptPrinter);
        atomicConceptPrinter.printNode(0,sortedAtomicConceptHierarchy.getBottomNode(),null,true);
    }
    public void printRoleHierarchy(Hierarchy<? extends Role> roleHierarchy,boolean objectProperties) {
        Hierarchy<Role> sortedRoleHierarchy=roleHierarchy.transform(new IdentityTransformer<Role>(),RoleComparator.INSTANCE);
        RolePrinter rolePrinter=new RolePrinter(sortedRoleHierarchy,objectProperties);
        sortedRoleHierarchy.traverseDepthFirst(rolePrinter);
        rolePrinter.printNode(0,sortedRoleHierarchy.getBottomNode(),null,true);
    }
    public void endPrinting() {
        m_out.println();
        m_out.println(")");
    }

    protected class AtomicConceptPrinter implements Hierarchy.HierarchyNodeVisitor<AtomicConcept> {
        protected final HierarchyNode<AtomicConcept> m_bottomNode;

        public AtomicConceptPrinter(HierarchyNode<AtomicConcept> bottomNode) {
            m_bottomNode=bottomNode;
        }
        public boolean redirect(HierarchyNode<AtomicConcept>[] nodes) {
            return true;
        }
        public void visit(int level,HierarchyNode<AtomicConcept> node,HierarchyNode<AtomicConcept> parentNode,boolean firstVisit) {
            if (!node.equals(m_bottomNode))
                printNode(level,node,parentNode,firstVisit);
        }
        public void printNode(int level,HierarchyNode<AtomicConcept> node,HierarchyNode<AtomicConcept> parentNode,boolean firstVisit) {
            Set<AtomicConcept> equivalences=node.getEquivalentElements();
            boolean printSubClasOf=(parentNode!=null);
            boolean printEquivalences=firstVisit && equivalences.size()>1;
            boolean printDeclarations=false;
            if (firstVisit)
                for (AtomicConcept atomicConcept : equivalences)
                    if (needsDeclaration(atomicConcept)) {
                        printDeclarations=true;
                        break;
                    }
            if (printSubClasOf || printEquivalences || printDeclarations) {
                for (int i=2*level;i>0;--i)
                    m_out.print(' ');
                boolean afterWS=true;
                if (printSubClasOf) {
                    m_out.print("SubClassOf( ");
                    print(node.getRepresentative());
                    m_out.print(' ');
                    print(parentNode.getRepresentative());
                    m_out.print(" )");
                    afterWS=false;
                }
                if (printEquivalences) {
                    if (!afterWS)
                        m_out.print(' ');
                    m_out.print("EquivalentClasses(");
                    for (AtomicConcept atomicConcept : equivalences) {
                        m_out.print(' ');
                        print(atomicConcept);
                    }
                    m_out.print(" )");
                    afterWS=false;
                }
                if (printDeclarations)
                    for (AtomicConcept atomicConcept : equivalences)
                        if (needsDeclaration(atomicConcept)) {
                            if (!afterWS)
                                m_out.print(' ');
                            m_out.print("Declaration( Class( ");
                            print(atomicConcept);
                            m_out.print(" ) )");
                            afterWS=false;
                        }
                m_out.println();
            }
        }
        protected void print(AtomicConcept atomicConcept) {
            m_out.print(m_prefixes.abbreviateURI(atomicConcept.getURI()));
        }
        protected boolean needsDeclaration(AtomicConcept atomicConcept) {
            return !AtomicConcept.NOTHING.equals(atomicConcept) && !AtomicConcept.THING.equals(atomicConcept);
        }
    }
    
    protected class RolePrinter implements Hierarchy.HierarchyNodeVisitor<Role> {
        protected final Hierarchy<Role> m_hierarchy;
        protected final boolean m_objectProperties;

        public RolePrinter(Hierarchy<Role> hierarchy,boolean objectProperties) {
            m_hierarchy=hierarchy;
            m_objectProperties=objectProperties;
        }
        public boolean redirect(HierarchyNode<Role>[] nodes) {
            if (isInverseRoleNode(nodes[0])) {
                HierarchyNode<Role> inverseParent=getInverseNode(nodes[1]);
                if (inverseParent.equals(nodes[1]))
                    return false;
                nodes[0]=getInverseNode(nodes[0]);
                nodes[1]=inverseParent;
            }
            return true;
        }
        public void visit(int level,HierarchyNode<Role> node,HierarchyNode<Role> parentNode,boolean firstVisit) {
            if (!node.equals(m_hierarchy.getBottomNode()))
                printNode(level,node,parentNode,firstVisit);
        }
        public void printNode(int level,HierarchyNode<Role> node,HierarchyNode<Role> parentNode,boolean firstVisit) {
            Set<Role> equivalences=node.getEquivalentElements();
            boolean printSubPropertyOf=(parentNode!=null);
            boolean printEquivalences=firstVisit && equivalences.size()>1;
            boolean printDeclarations=false;
            if (firstVisit)
                for (Role role : equivalences)
                    if (needsDeclaration(role)) {
                        printDeclarations=true;
                        break;
                    }
            if (printSubPropertyOf || printEquivalences || printDeclarations) {
                for (int i=2*level;i>0;--i)
                    m_out.print(' ');
                boolean afterWS=true;
                if (printSubPropertyOf) {
                    if (m_objectProperties)
                        m_out.print("SubObjectPropertyOf( ");
                    else
                        m_out.print("SubDataPropertyOf( ");
                    print(node.getRepresentative());
                    m_out.print(' ');
                    print(parentNode.getRepresentative());
                    m_out.print(" )");
                    afterWS=false;
                }
                if (printEquivalences) {
                    if (!afterWS)
                        m_out.print(' ');
                    if (m_objectProperties)
                        m_out.print("EquivalentObjectProperties(");
                    else
                        m_out.print("EquivalentDataProperties(");
                    for (Role role : equivalences) {
                        m_out.print(' ');
                        print(role);
                    }
                    m_out.print(" )");
                    afterWS=false;
                }
                if (printDeclarations)
                    for (Role role : equivalences)
                        if (needsDeclaration(role)) {
                            if (!afterWS)
                                m_out.print(' ');
                            m_out.print("Declaration( ");
                            if (m_objectProperties)
                                m_out.print("ObjectProperty( ");
                            else
                                m_out.print("DataProperty( ");
                            print(role);
                            m_out.print(" ) )");
                            afterWS=false;
                        }
                m_out.println();
            }
        }
        protected boolean isInverseRoleNode(HierarchyNode<Role> node) {
            return node.getRepresentative() instanceof InverseRole;
        }
        protected HierarchyNode<Role> getInverseNode(HierarchyNode<Role> node) {
            Role redirectTo=node.getRepresentative().getInverse();
            return m_hierarchy.getNodeForElement(redirectTo);
        }
        protected void print(Role role) {
            if (role instanceof AtomicRole)
                m_out.print(m_prefixes.abbreviateURI(((AtomicRole)role).getURI()));
            else {
                m_out.print("ObjectInverseOf( ");
                print(((InverseRole)role).getInverseOf());
                m_out.print(" )");
            }
        }
        protected void print(AtomicRole atomicRole) {
            m_out.print(m_prefixes.abbreviateURI(atomicRole.getURI()));
        }
        protected boolean needsDeclaration(Role role) {
            return !AtomicRole.BOTTOM_OBJECT_ROLE.equals(role) && !AtomicRole.TOP_OBJECT_ROLE.equals(role) && !AtomicRole.BOTTOM_DATA_ROLE.equals(role) && !AtomicRole.TOP_DATA_ROLE.equals(role) && role instanceof AtomicRole;
        }
    }
    
    protected static class RoleComparator implements Comparator<Role> {
        public static final RoleComparator INSTANCE=new RoleComparator();

        public int compare(Role role1,Role role2) {
            int comparison=getRoleClass(role1)-getRoleClass(role2);
            if (comparison!=0)
                return comparison;
            comparison=getRoleDirection(role1)-getRoleDirection(role2);
            if (comparison!=0)
                return comparison;
            return getInnerAtomicRole(role1).getURI().compareTo(getInnerAtomicRole(role2).getURI());
        }
        protected int getRoleClass(Role role) {
            if (AtomicRole.BOTTOM_OBJECT_ROLE.equals(role))
                return 0;
            else if (AtomicRole.TOP_OBJECT_ROLE.equals(role))
                return 1;
            else if (AtomicRole.BOTTOM_DATA_ROLE.equals(role))
                return 2;
            else if (AtomicRole.TOP_DATA_ROLE.equals(role))
                return 3;
            else
                return 4;
        }
        protected AtomicRole getInnerAtomicRole(Role role) {
            if (role instanceof AtomicRole)
                return (AtomicRole)role;
            else
                return ((InverseRole)role).getInverseOf();
        }
        protected int getRoleDirection(Role role) {
            return role instanceof AtomicRole ? 0 : 1;
        }
    }
    
    protected static class AtomicConceptComparator implements Comparator<AtomicConcept> {
        public static final AtomicConceptComparator INSTANCE=new AtomicConceptComparator();
    
        public int compare(AtomicConcept atomicConcept1,AtomicConcept atomicConcept2) {
            int comparison=getAtomicConceptClass(atomicConcept1)-getAtomicConceptClass(atomicConcept2);
            if (comparison!=0)
                return comparison;
            return atomicConcept1.getURI().compareTo(atomicConcept2.getURI());
        }
        protected int getAtomicConceptClass(AtomicConcept atomicConcept) {
            if (AtomicConcept.NOTHING.equals(atomicConcept))
                return 0;
            else if (AtomicConcept.THING.equals(atomicConcept))
                return 1;
            else
                return 2;
        }
    }

    protected class IdentityTransformer<E> implements Hierarchy.Transformer<E,E> {

        public E transform(E object) {
            return object;
        }
        public E determineRepresentative(E oldRepresentative,Set<E> newEquivalentElements) {
            return ((SortedSet<E>)newEquivalentElements).first();
        }
    }
}
