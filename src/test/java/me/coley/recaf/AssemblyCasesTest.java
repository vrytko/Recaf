package me.coley.recaf;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import org.junit.jupiter.api.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;

import static org.junit.jupiter.api.Assertions.*;

/**
 * More verbose cases for the assembler.
 *
 * @author Matt
 */
public class AssemblyCasesTest {
	private static final String D1 = "DEFINE static func()V\nSTART:\n";
	private static final String D2 = "\nEND:\n";

	@Nested
	public class Examples {
		@Test
		public void testHelloWorld() {
			String s = "DEFINE public static hi()V\n" +
					"A:\n" +
					"LINE A 4\n" +
					"GETSTATIC java/lang/System.out Ljava/io/PrintStream;\n" +
					"LDC \"Hello world\"\n" +
					"INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V\n" +
					"B:\n" +
					"LINE B 5\n" +
					"RETURN";
			verifyPass(Parse.parse(s));
		}
	}

	@Nested
	public class Variables {
		@Test
		public void testRawIndices() {
			try {
				MethodNode node = compile(parse(
						"ICONST_0\nISTORE 0\n" +
						"ICONST_0\nISTORE 1\n" +
						"ICONST_0\nISTORE 2\n"));
				assertEquals(0, ((VarInsnNode) node.instructions.get(2)).var);
				assertEquals(1, ((VarInsnNode) node.instructions.get(4)).var);
				assertEquals(2, ((VarInsnNode) node.instructions.get(6)).var);
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testNamedIndices() {
			try {
				MethodNode node = compile(parse(
						"ICONST_0\nISTORE zero\n" +
						"ICONST_0\nISTORE one\n" +
						"ICONST_0\nISTORE two\n"));
				assertEquals(0, ((VarInsnNode) node.instructions.get(2)).var);
				assertEquals(1, ((VarInsnNode) node.instructions.get(4)).var);
				assertEquals(2, ((VarInsnNode) node.instructions.get(6)).var);
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testMixedIndices() {
			try {
				MethodNode node = compile(parse(
						"ICONST_0\nISTORE 0\n" +
						"ICONST_0\nISTORE k\n" +
						"ICONST_0\nISTORE 2\n"));
				assertEquals(0, ((VarInsnNode) node.instructions.get(2)).var);
				assertEquals(1, ((VarInsnNode) node.instructions.get(4)).var);
				assertEquals(2, ((VarInsnNode) node.instructions.get(6)).var);
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}
	}

	@Nested
	public class Verify {
		@Test
		public void testStoreObjInInt() {
			try {
				verifyFails(parse("ACONST_NULL\nISTORE 0\nRETURN"));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testStoreIntAsObj() {
			try {
				verifyFails(parse("ICONST_0\nASTORE 0\nRETURN"));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testVoidHasObjReturn() {
			try {
				verifyFails(parse("ACONST_NULL\nARETURN"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testReferenceOnPrimitive() {
			try {
				verifyFails(parse("ICONST_0\nINVOKEVIRTUAL owner.name()V\nRETURN"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testStoreDoubleInInt() {
			try {
				verifyFails(parse("DCONST_1\nISTORE test\nRETURN"));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testStoreDoubleInInt2() {
			try {
				verifyFails(parse("INVOKESTATIC test.get()D\nISTORE test\nRETURN"));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testInvokeOnNull() {
			try {
				verifyFails(parse("ACONST_NULL\nICONST_0\nINVOKEVIRTUAL test.get(I)V\nRETURN"));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testPopOnEmpty() {
			try {
				verifyFails(parse("POP\nRETURN"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testPop2On1Size() {
			try {
				verifyFails(parse("ACONST_NULL\nPOP2\nRETURN"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testMissingReturn() {
			try {
				verifyFails(parse("NOP"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testStoreInWideVariableTypesReservedSpace() {
			try {
				// Double takes two spots, 0 and 1
				// Should fail if we try to save to 1
				verifyFails(parse("DCONST_0\nDSTORE 0\nICONST_0\nISTORE 1\nRETURN"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}
	}

	// =============================================================== //

	private MethodNode compile(ParseResult<RootAST> result) throws AssemblerException {
		Assembler assembler = new Assembler("Test");
		return assembler.compile(result);
	}

	private Frame<RValue>[] verify(ParseResult<RootAST> result) throws AssemblerException {
		Assembler assembler = new Assembler("Test");
		MethodNode node = assembler.compile(result);
		try {
			return assembler.verify(node);
		} catch(AssemblerException ex) {
			fail(ex);
			return null;
		}
	}

	private void verifyFails(ParseResult<RootAST> result) throws AssemblerException {
		Assembler assembler = new Assembler("Test");
		MethodNode node = assembler.compile(result);
		try {
			assembler.verify(node);
			fail("Code did not throw any verification exceptions");
		} catch(AssemblerException ex) {
			System.err.println(ex.getMessage());
		}
	}

	private void verifyPass(ParseResult<RootAST> result) {
		Assembler assembler = new Assembler("Test");
		try {
			MethodNode node = assembler.compile(result);
			assembler.verify(node);
		} catch(AssemblerException ex) {
			fail(ex);
		}
	}

	private static ParseResult<RootAST> parse(String code) {
		return Parse.parse(D1 + code + D2);
	}
}
