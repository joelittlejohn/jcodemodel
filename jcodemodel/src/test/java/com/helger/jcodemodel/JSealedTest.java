/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright 2013-2025 Philip Helger + contributors
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.helger.jcodemodel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.jcodemodel.exceptions.JCodeModelException;
import com.helger.jcodemodel.util.CodeModelTestsHelper;

/**
 * Test class for Java sealed types support (JEP 409, Java 17+).
 *
 * Sealed classes and interfaces restrict which other classes or interfaces
 * may extend or implement them. This provides more control over inheritance
 * hierarchies and enables exhaustive pattern matching.
 *
 * Key concepts:
 * - sealed: Restricts subclasses to those listed in the permits clause
 * - non-sealed: A permitted subclass that opens up the hierarchy again
 * - final: A permitted subclass that allows no further extension
 * - permits: Lists the permitted subclasses/subinterfaces
 */
public final class JSealedTest
{
  /**
   * Test: Basic sealed class with permits clause
   *
   * Expected output:
   * <pre>
   * public sealed class Shape permits Circle, Rectangle {
   * }
   * </pre>
   */
  @Test
  public void testBasicSealedClass () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    final JDefinedClass shape = pkg._class (JMod.PUBLIC | JMod.SEALED, "Shape");
    final JDefinedClass circle = pkg._class (JMod.PUBLIC | JMod.FINAL, "Circle");
    circle._extends (shape);
    final JDefinedClass rectangle = pkg._class (JMod.PUBLIC | JMod.FINAL, "Rectangle");
    rectangle._extends (shape);

    shape.permits (circle, rectangle);

    final String output = CodeModelTestsHelper.declare (shape);
    assertTrue (output.contains ("sealed"));
    assertTrue (output.contains ("permits"));
    assertTrue (output.contains ("Circle"));
    assertTrue (output.contains ("Rectangle"));

    assertTrue (shape.isSealed ());
    assertFalse (shape.isNonSealed ());

    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Sealed interface with permits clause
   *
   * Expected output:
   * <pre>
   * public sealed interface Expr permits ConstantExpr, BinaryExpr {
   * }
   * </pre>
   */
  @Test
  public void testSealedInterface () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    final JDefinedClass expr = pkg._class (JMod.PUBLIC | JMod.SEALED, "Expr", EClassType.INTERFACE);
    final JDefinedClass constantExpr = pkg._class (JMod.PUBLIC | JMod.FINAL, "ConstantExpr");
    constantExpr._implements (expr);
    final JDefinedClass binaryExpr = pkg._class (JMod.PUBLIC | JMod.FINAL, "BinaryExpr");
    binaryExpr._implements (expr);

    expr.permits (constantExpr, binaryExpr);

    final String output = CodeModelTestsHelper.declare (expr);
    assertTrue (output.contains ("sealed interface Expr"));
    assertTrue (output.contains ("permits"));

    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Non-sealed subclass of a sealed class
   *
   * Expected output:
   * <pre>
   * public non-sealed class Square extends Shape {
   * }
   * </pre>
   */
  @Test
  public void testNonSealedClass () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    final JDefinedClass shape = pkg._class (JMod.PUBLIC | JMod.SEALED, "Shape");
    final JDefinedClass square = pkg._class (JMod.PUBLIC | JMod.NON_SEALED, "Square");
    square._extends (shape);

    shape.permits (square);

    final String output = CodeModelTestsHelper.declare (square);
    assertTrue (output.contains ("non-sealed"));

    assertFalse (square.isSealed ());
    assertTrue (square.isNonSealed ());

    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Sealed record implementing sealed interface
   *
   * Expected output:
   * <pre>
   * public sealed interface Node permits Leaf, Branch {
   * }
   * public record Leaf(int value) implements Node {
   * }
   * </pre>
   */
  @Test
  public void testSealedInterfaceWithRecord () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    final JDefinedClass node = pkg._class (JMod.PUBLIC | JMod.SEALED, "Node", EClassType.INTERFACE);
    final JDefinedClass leaf = pkg._record ("Leaf");
    leaf.recordComponent (cm.INT, "value");
    leaf._implements (node);
    final JDefinedClass branch = pkg._record ("Branch");
    branch.recordComponent (node, "left");
    branch.recordComponent (node, "right");
    branch._implements (node);

    node.permits (leaf, branch);

    final String nodeOutput = CodeModelTestsHelper.declare (node);
    assertTrue (nodeOutput.contains ("sealed interface Node"));
    assertTrue (nodeOutput.contains ("permits"));

    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Chained sealed hierarchy (sealed -> sealed -> final)
   *
   * Expected output:
   * <pre>
   * public sealed class Vehicle permits Car, Truck {
   * }
   * public sealed class Car extends Vehicle permits Sedan, Coupe {
   * }
   * public final class Sedan extends Car {
   * }
   * </pre>
   */
  @Test
  public void testChainedSealedHierarchy () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    // Top level sealed class
    final JDefinedClass vehicle = pkg._class (JMod.PUBLIC | JMod.SEALED, "Vehicle");

    // Second level sealed class
    final JDefinedClass car = pkg._class (JMod.PUBLIC | JMod.SEALED, "Car");
    car._extends (vehicle);

    // Also permit Truck at top level
    final JDefinedClass truck = pkg._class (JMod.PUBLIC | JMod.FINAL, "Truck");
    truck._extends (vehicle);

    vehicle.permits (car, truck);

    // Third level final classes
    final JDefinedClass sedan = pkg._class (JMod.PUBLIC | JMod.FINAL, "Sedan");
    sedan._extends (car);
    final JDefinedClass coupe = pkg._class (JMod.PUBLIC | JMod.FINAL, "Coupe");
    coupe._extends (car);

    car.permits (sedan, coupe);

    final String vehicleOutput = CodeModelTestsHelper.declare (vehicle);
    assertTrue (vehicleOutput.contains ("sealed class Vehicle"));
    assertTrue (vehicleOutput.contains ("permits"));

    final String carOutput = CodeModelTestsHelper.declare (car);
    assertTrue (carOutput.contains ("sealed class Car"));
    assertTrue (carOutput.contains ("extends"));
    assertTrue (carOutput.contains ("permits"));

    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Sealed class with both extends and permits
   *
   * Expected output:
   * <pre>
   * public sealed class SpecialShape extends Shape permits SpecialCircle {
   * }
   * </pre>
   */
  @Test
  public void testSealedWithExtendsAndPermits () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    final JDefinedClass shape = pkg._class (JMod.PUBLIC | JMod.SEALED, "Shape");
    final JDefinedClass specialShape = pkg._class (JMod.PUBLIC | JMod.SEALED, "SpecialShape");
    specialShape._extends (shape);
    final JDefinedClass specialCircle = pkg._class (JMod.PUBLIC | JMod.FINAL, "SpecialCircle");
    specialCircle._extends (specialShape);

    shape.permits (specialShape);
    specialShape.permits (specialCircle);

    final String output = CodeModelTestsHelper.declare (specialShape);
    assertTrue (output.contains ("sealed class SpecialShape"));
    assertTrue (output.contains ("extends"));
    assertTrue (output.contains ("permits"));

    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Sealed class with implements and permits
   *
   * Expected output:
   * <pre>
   * public sealed class ConcreteExpr implements Expr permits AddExpr, MulExpr {
   * }
   * </pre>
   */
  @Test
  public void testSealedWithImplementsAndPermits () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    final JDefinedClass expr = pkg._class (JMod.PUBLIC, "Expr", EClassType.INTERFACE);
    final JDefinedClass concreteExpr = pkg._class (JMod.PUBLIC | JMod.SEALED, "ConcreteExpr");
    concreteExpr._implements (expr);

    final JDefinedClass addExpr = pkg._class (JMod.PUBLIC | JMod.FINAL, "AddExpr");
    addExpr._extends (concreteExpr);
    final JDefinedClass mulExpr = pkg._class (JMod.PUBLIC | JMod.FINAL, "MulExpr");
    mulExpr._extends (concreteExpr);

    concreteExpr.permits (addExpr, mulExpr);

    final String output = CodeModelTestsHelper.declare (concreteExpr);
    assertTrue (output.contains ("sealed class ConcreteExpr"));
    assertTrue (output.contains ("implements"));
    assertTrue (output.contains ("permits"));

    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Sealed interface extending another interface
   *
   * Expected output:
   * <pre>
   * public sealed interface SpecialExpr extends Expr permits LiteralExpr {
   * }
   * </pre>
   */
  @Test
  public void testSealedInterfaceExtendingInterface () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    final JDefinedClass expr = pkg._class (JMod.PUBLIC, "Expr", EClassType.INTERFACE);
    final JDefinedClass specialExpr = pkg._class (JMod.PUBLIC | JMod.SEALED, "SpecialExpr", EClassType.INTERFACE);
    specialExpr._extends (expr);

    final JDefinedClass literalExpr = pkg._class (JMod.PUBLIC | JMod.FINAL, "LiteralExpr");
    literalExpr._implements (specialExpr);

    specialExpr.permits (literalExpr);

    final String output = CodeModelTestsHelper.declare (specialExpr);
    assertTrue (output.contains ("sealed interface SpecialExpr"));
    assertTrue (output.contains ("extends"));
    assertTrue (output.contains ("permits"));

    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Multiple permitted subclasses
   *
   * Expected output:
   * <pre>
   * public sealed class Result permits Success, Failure, Pending, Cancelled {
   * }
   * </pre>
   */
  @Test
  public void testMultiplePermittedSubclasses () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    final JDefinedClass result = pkg._class (JMod.PUBLIC | JMod.SEALED, "Result");
    final JDefinedClass success = pkg._class (JMod.PUBLIC | JMod.FINAL, "Success");
    success._extends (result);
    final JDefinedClass failure = pkg._class (JMod.PUBLIC | JMod.FINAL, "Failure");
    failure._extends (result);
    final JDefinedClass pending = pkg._class (JMod.PUBLIC | JMod.FINAL, "Pending");
    pending._extends (result);
    final JDefinedClass cancelled = pkg._class (JMod.PUBLIC | JMod.FINAL, "Cancelled");
    cancelled._extends (result);

    result.permits (success, failure, pending, cancelled);

    final String output = CodeModelTestsHelper.declare (result);
    assertTrue (output.contains ("permits"));
    assertTrue (output.contains ("Success"));
    assertTrue (output.contains ("Failure"));
    assertTrue (output.contains ("Pending"));
    assertTrue (output.contains ("Cancelled"));

    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Nested sealed class
   *
   * Expected output:
   * <pre>
   * public class Outer {
   *     public sealed class Inner permits InnerChild {
   *     }
   *     public final class InnerChild extends Inner {
   *     }
   * }
   * </pre>
   */
  @Test
  public void testNestedSealedClass () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    final JDefinedClass outer = pkg._class (JMod.PUBLIC, "Outer");
    final JDefinedClass inner = outer._class (JMod.PUBLIC | JMod.SEALED, "Inner");
    final JDefinedClass innerChild = outer._class (JMod.PUBLIC | JMod.FINAL, "InnerChild");
    innerChild._extends (inner);

    inner.permits (innerChild);

    final String output = CodeModelTestsHelper.declare (outer);
    assertTrue (output.contains ("sealed class Inner"));
    assertTrue (output.contains ("permits"));

    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Generic sealed class
   *
   * Expected output:
   * <pre>
   * public sealed class Option&lt;T&gt; permits Some, None {
   * }
   * </pre>
   */
  @Test
  public void testGenericSealedClass () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    final JDefinedClass option = pkg._class (JMod.PUBLIC | JMod.SEALED, "Option");
    option.generify ("T");

    final JDefinedClass some = pkg._class (JMod.PUBLIC | JMod.FINAL, "Some");
    some.generify ("T");
    some._extends (option.narrow (some.typeParams ()[0]));

    final JDefinedClass none = pkg._class (JMod.PUBLIC | JMod.FINAL, "None");
    none.generify ("T");
    none._extends (option.narrow (none.typeParams ()[0]));

    option.permits (some, none);

    final String output = CodeModelTestsHelper.declare (option);
    assertTrue (output.contains ("sealed class Option<T>"));
    assertTrue (output.contains ("permits"));

    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Records as permitted subtypes of a sealed interface
   *
   * Note: Records themselves cannot be sealed because they are implicitly final
   * and cannot be extended. However, records can implement sealed interfaces.
   *
   * Expected output:
   * <pre>
   * public sealed interface Event permits ClickEvent, KeyEvent {
   * }
   * public record ClickEvent(int x, int y) implements Event {
   * }
   * </pre>
   */
  @Test
  public void testRecordsAsPermittedSubtypes () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    // Sealed interface that permits record implementations
    final JDefinedClass event = pkg._class (JMod.PUBLIC | JMod.SEALED, "Event", EClassType.INTERFACE);

    final JDefinedClass clickEvent = pkg._record ("ClickEvent");
    clickEvent.recordComponent (cm.INT, "x");
    clickEvent.recordComponent (cm.INT, "y");
    clickEvent._implements (event);

    final JDefinedClass keyEvent = pkg._record ("KeyEvent");
    keyEvent.recordComponent (cm.CHAR, "key");
    keyEvent._implements (event);

    event.permits (clickEvent, keyEvent);

    final String output = CodeModelTestsHelper.declare (event);
    assertTrue (output.contains ("sealed interface Event"));
    assertTrue (output.contains ("permits"));

    // Records are implicitly final, so they don't need special modifiers
    // when implementing a sealed interface
    CodeModelTestsHelper.parseCodeModel (cm);
  }

  /**
   * Test: Verify isSealed() and isNonSealed() methods
   */
  @Test
  public void testSealedAndNonSealedMethods () throws JCodeModelException
  {
    final JCodeModel cm = new JCodeModel ();
    final JPackage pkg = cm._package ("org.example");

    final JDefinedClass sealed = pkg._class (JMod.PUBLIC | JMod.SEALED, "Sealed");
    final JDefinedClass nonSealed = pkg._class (JMod.PUBLIC | JMod.NON_SEALED, "NonSealed");
    nonSealed._extends (sealed);
    final JDefinedClass finalClass = pkg._class (JMod.PUBLIC | JMod.FINAL, "FinalClass");
    finalClass._extends (sealed);
    final JDefinedClass regular = pkg._class (JMod.PUBLIC, "Regular");

    sealed.permits (nonSealed, finalClass);

    assertTrue (sealed.isSealed ());
    assertFalse (sealed.isNonSealed ());

    assertFalse (nonSealed.isSealed ());
    assertTrue (nonSealed.isNonSealed ());

    assertFalse (finalClass.isSealed ());
    assertFalse (finalClass.isNonSealed ());

    assertFalse (regular.isSealed ());
    assertFalse (regular.isNonSealed ());

    CodeModelTestsHelper.parseCodeModel (cm);
  }
}
