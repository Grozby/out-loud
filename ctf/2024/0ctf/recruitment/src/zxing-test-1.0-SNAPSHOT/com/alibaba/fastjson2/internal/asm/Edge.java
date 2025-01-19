package com.alibaba.fastjson2.internal.asm;

final class Edge {
   final Label successor;
   final Edge nextEdge;

   Edge(Label successor, Edge nextEdge) {
      this.successor = successor;
      this.nextEdge = nextEdge;
   }
}
