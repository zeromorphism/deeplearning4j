package org.deeplearning4j.graph.models;

/** Binary tree interface, used in DeepWalk */
public interface BinaryTree {

    long getCode(int element);

    int getCodeLength(int element);

    String getCodeString(int element);

    int[] getPathInnerNodes(int element);
}
