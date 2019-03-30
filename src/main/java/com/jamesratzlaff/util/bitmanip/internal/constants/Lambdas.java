/**
 * 
 */
package com.jamesratzlaff.util.bitmanip.internal.constants;

import java.util.function.IntBinaryOperator;
import java.util.function.ToIntFunction;

import com.jamesratzlaff.util.function.ObjIntByteConsumer;
import com.jamesratzlaff.util.function.ObjIntToByteFunction;

/**
 * @author James Ratzlaff
 *
 */
public class Lambdas {

	public static final class ofType {
		public static enum IntBinaryOp implements IntBinaryOperator {
			LT((a, b) -> a < b ? 1 : 0),
			GT((a, b) -> a > b ? 1 : 0),
			URSH((a, b) -> a >>> b),
			LSH((a, b) -> a << b),
			ADD((a, b) -> a + b),
			SUB((a, b) -> a - b),
			XOR((a,b) -> a^b),
			AND((a,b)->a&b),
			OR((a,b)->a|b);
			private final IntBinaryOperator op;

			private IntBinaryOp(IntBinaryOperator op) {
				this.op = op;
			}

			@Override
			public int applyAsInt(int left, int right) {
				return this.op.applyAsInt(left, right);
			}
		}
	}

	public static final class forType {
		public static final class byteArray {
			public static final ToIntFunction<byte[]> lengthGetter = bytes -> bytes.length;
			public static final ObjIntToByteFunction<byte[]> getAtIndex = (bytes, i) -> bytes[i];
			public static final ObjIntByteConsumer<byte[]> setAtIndex = (bytes, i, b) -> bytes[i] = b;
		}
	}

}
