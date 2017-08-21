/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hll;

import static com.yahoo.sketches.hll.HllUtil.LG_AUX_ARR_INTS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.SketchesStateException;

/**
 * @author Lee Rhodes
 */
public class AuxHashMapTest {
  static final String LS = System.getProperty("line.separator");

  @Test
  public void exerciseAux() {
    int lgK = 15; //this combination should create an Aux with ~18 exceptions
    int lgU = 20;
    println("HLL_4, lgK: " + lgK + ", lgU: " + lgU);

    HllSketch sketch = new HllSketch(lgK, TgtHllType.HLL_4);
    for (int i = 0; i < (1 << lgU); i++) { sketch.update(i); }

    //check Ser Bytes
    assertEquals(sketch.getUpdatableSerializationBytes(), 40 + (1 << (lgK - 1))
        + (4 << LG_AUX_ARR_INTS[lgK]) );

    AbstractHllArray absHll = (AbstractHllArray) sketch.hllSketchImpl;
    int curMin = absHll.getCurMin();

    println("CurMin: " + curMin);
    PairIterator itr = absHll.getAuxIterator();
    println("Aux Array before SerDe.");
    println(itr.getHeader());
    while (itr.nextValid()) {
      println(itr.getString());
    }

    byte[] byteArr = sketch.toUpdatableByteArray();
    HllSketch sk2 = HllSketch.heapify(Memory.wrap(byteArr));
    assertEquals(sketch.getEstimate(), sk2.getEstimate());

    assertEquals(sketch.getUpdatableSerializationBytes(), 40 + (1 << (lgK - 1))
        + (4 << LG_AUX_ARR_INTS[lgK]));

    PairIterator h4itr = sketch.getIterator();
    println("\nMain Array: where (value - curMin) > 14. key/vals should match above.");
    println(h4itr.getHeader());
    while (h4itr.nextValid()) {
      if ((h4itr.getValue() - curMin) > 14) {
        println(h4itr.getString());
      }
    }
    h4itr = absHll.getAuxIterator();
    println("\nAux Array after SerDe: should match above.");
    println(h4itr.getHeader());
    while (h4itr.nextAll()) {
      if (h4itr.getValue() > 14) {
        println(h4itr.getString());
      }
    }
    sketch.toString(true, true, true, false);
  }

  @Test
  public void checkMustReplace() {
    HeapAuxHashMap map = new HeapAuxHashMap(3, 7);
    map.mustAdd(100, 5);
    int val = map.mustFindValueFor(100);
    assertEquals(val, 5);

    map.mustReplace(100, 10);
    val = map.mustFindValueFor(100);
    assertEquals(val, 10);

    try {
      map.mustReplace(101, 5);
      fail();
    } catch (SketchesStateException e) {
      //expected
    }
  }

  @Test
  public void checkGrowSpace() {
    HeapAuxHashMap map = new HeapAuxHashMap(3, 7);
    assertFalse(map.isMemory());
    assertFalse(map.isOffHeap());
    assertEquals(map.getLgAuxArrInts(), 3);
    for (int i = 1; i <= 7; i++) {
      map.mustAdd(i, i);
    }
    assertEquals(map.getLgAuxArrInts(), 4);
    PairIterator itr = map.getIterator();
    int count1 = 0;
    int count2 = 0;
    while (itr.nextAll()) {
      count2++;
      int pair = itr.getPair();
      if (pair != 0) { count1++; }
    }
    assertEquals(count1, 7);
    assertEquals(count2, 16);
  }

  @Test(expectedExceptions = SketchesStateException.class)
  public void checkExceptions1() {
    HeapAuxHashMap map = new HeapAuxHashMap(3, 7);
    map.mustAdd(100, 5);
    map.mustFindValueFor(101);
  }

  @Test(expectedExceptions = SketchesStateException.class)
  public void checkExceptions2() {
    HeapAuxHashMap map = new HeapAuxHashMap(3, 7);
    map.mustAdd(100, 5);
    map.mustAdd(100, 6);
  }

  @Test
  public void printlnTest() {
    println("PRINTING: " + this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    //System.out.println(s); //disable here
  }

}
