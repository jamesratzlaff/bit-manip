/**
 * 
 */
package com.jamesratzlaff.util.bitmanip;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.jamesratzlaff.util.function.IntToByteFunction;
import com.jamesratzlaff.util.function.ObjIntByteConsumer;
import com.jamesratzlaff.util.function.ObjIntToByteBiFunction;

/**
 * @author James Ratzlaff
 *
 */
public class Bitwise {

	public static final class Constants {
		private static final class IntBinaryOps{
		private static final IntBinaryOperator aLessThanB = (a, b) -> a < b ? 1 : 0;
		private static final IntBinaryOperator aGreaterThanB = (a, b) -> a > b ? 1 : 0;
		private static final IntBinaryOperator unsignedRightShift = (a, b) -> a >>> b;
		private static final IntBinaryOperator leftShift = (a, b) -> a << b;
		private static final IntBinaryOperator add = (a, b) -> a + b;
		private static final IntBinaryOperator sub = (a, b) -> a - b;
		}
	}

//	public static void cyclicShift(byte[] bytes, int amount) {
//		cyclicShift(bytes, amount, 0, Integer.MAX_VALUE);
//	}
//
//	public static void cyclicShift(byte[] bytes, int amount, int from, int to) {
//		int bytesLength = bytes != null ? bytes.length : 0;
//		if (amount != 0 && bytesLength > 0) {
//			int dataWidth = Byte.SIZE;
//			int startIdx = Math.max(Math.min(from, to), 0);
//			int endIdx = Math.min(Math.max(from, to), bytesLength);
//			int numberOfBytes = (endIdx - startIdx);
//			if (numberOfBytes > 0) {
//				amount = amount % (numberOfBytes * dataWidth);
//				IntBinaryOperator preLoopOp = amount < 0 ? add : sub;
//
//				if (Math.abs(amount) > dataWidth) {
//					while (Math.abs(amount) > dataWidth) {
//						cyclicShift(bytes, dataWidth, startIdx, endIdx);
//						amount = preLoopOp.applyAsInt(amount, dataWidth);
//					}
//				}
//
//				int start = amount > 0 ? startIdx : endIdx - 1;
//				int end = start == startIdx ? endIdx : -1;
//				int direction = start < end ? 1 : -1;
//				amount = Math.abs(amount);
//				boolean iteratesForward = direction == 1;
//				int carryMask = iteratesForward ? ((1 << (amount)) - 1) : (((byte) -127) >> amount);
//				int backShift = (dataWidth - amount);
//
//				IntBinaryOperator backShiftOp = iteratesForward ? leftShift : unsignedRightShift;
//				IntBinaryOperator shiftOp = iteratesForward ? unsignedRightShift : leftShift;
//				int previousCarry = 0;
//				IntBinaryOperator endCondition = iteratesForward ? aLessThanB : aGreaterThanB;
//				for (int i = start; endCondition.applyAsInt(i, end) == 1; i += direction) {
//					int current = Byte.toUnsignedInt(bytes[i]);
//					int carry = (current & carryMask);
//					carry = backShiftOp.applyAsInt(carry, backShift) & 0xFF;
//					current = shiftOp.applyAsInt(current, amount) & 0xFF;
//					bytes[i] = (byte) (current | previousCarry);
//					previousCarry = carry;
//				}
//				bytes[start] = (byte) (bytes[start] | previousCarry);
//			}
//		}
//	}

	public static <T> void cyclicShift(T bytes, int amount, int from, int to, ToIntFunction<T> lengthGetter,
			ObjIntToByteBiFunction<T> getAtIndex, ObjIntByteConsumer<T> setAtIndex) {
		if (Arrays.asList(lengthGetter, getAtIndex, setAtIndex).stream().anyMatch(Objects::isNull)) {
			return;
		}
		int bytesLength = bytes != null ? lengthGetter.applyAsInt(bytes) : 0;
		if (amount != 0 && bytesLength > 0) {
			final int dataWidth = Byte.SIZE;
			final int startIdx = Math.max(Math.min(from, to), 0);
			final int endIdx = Math.min(Math.max(from, to), bytesLength);
			final int numberOfBytes = (endIdx - startIdx);
			if (numberOfBytes > 0) {
				amount = amount % (numberOfBytes * dataWidth);
				final IntBinaryOperator preLoopOp = amount < 0 ? Constants.IntBinaryOps.add : Constants.IntBinaryOps.sub;

				if (Math.abs(amount) > dataWidth) {
					while (Math.abs(amount) > dataWidth) {
						cyclicShift(bytes, dataWidth, startIdx, endIdx, lengthGetter, getAtIndex, setAtIndex);
						amount = preLoopOp.applyAsInt(amount, dataWidth);
					}
				}

				int start = amount > 0 ? startIdx : endIdx - 1;
				int end = start == startIdx ? endIdx : -1;
				int direction = start < end ? 1 : -1;
				amount = Math.abs(amount);
				final boolean iteratesForward = direction == 1;
				final int carryMask = iteratesForward ? ((1 << (amount)) - 1) : (((byte) -127) >> amount);
				final int backShift = (dataWidth - amount);
				final IntBinaryOperator backShiftOp = iteratesForward ? Constants.IntBinaryOps.leftShift : Constants.IntBinaryOps.unsignedRightShift;
				final IntBinaryOperator shiftOp = iteratesForward ? Constants.IntBinaryOps.unsignedRightShift : Constants.IntBinaryOps.leftShift;
				int previousCarry = 0;
				final IntBinaryOperator endCondition = iteratesForward ? Constants.IntBinaryOps.aLessThanB : Constants.IntBinaryOps.aGreaterThanB;
				for (int i = start; endCondition.applyAsInt(i, end) == 1; i += direction) {
					int current = Byte.toUnsignedInt(getAtIndex.applyAsByte(bytes, i));
					int carry = (current & carryMask);
					carry = backShiftOp.applyAsInt(carry, backShift) & 0xFF;
					current = shiftOp.applyAsInt(current, amount) & 0xFF;
					setAtIndex.accept(bytes, i, (byte) (current | previousCarry));
					previousCarry = carry;
				}
				setAtIndex.accept(bytes, start, (byte) (getAtIndex.applyAsByte(bytes, start) | previousCarry));
			}
		}
	}



	private static final int[] toIntArray(byte[] bytes) {
		int[] asInts = new int[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			asInts[i] = Byte.toUnsignedInt(bytes[i]);
		}
		return asInts;
	}

	private static String pad(String str) {
		int diff = Byte.SIZE - str.length();
		if (diff > 0) {
			char[] filler = new char[diff];
			Arrays.fill(filler, '0');
			return new StringBuilder(Byte.SIZE).append(filler).append(str).toString();
		}
		return str;
	}

	private static final String asByteBinary(int i) {
		return pad(Integer.toBinaryString(i));
	}

	private static void printBinaryString(String label, int b) {
		System.out.println(toBinaryString(label, (byte) b));
	}

	private static void printBinaryString(String label, byte b) {
		System.out.println(toBinaryString(label, b));
	}

	public static String toBinaryString(String label, byte b) {
		return label + toBinaryString(b);
	}

	public static String toBinaryString(byte b) {
		return asByteBinary(Byte.toUnsignedInt(b));
	}

	public static String toBinaryString(byte[] bytes) {
		return String.join("",
				Arrays.stream(toIntArray(bytes)).mapToObj(Bitwise::asByteBinary).collect(Collectors.toList()));
	}

	public static void main(String[] args) {
//		byte[] bytes = new byte[] { (byte) 0x81, 24, 7 };
//		System.out.println(toBinaryString(bytes));
//		for (int i = 0; i < 9; i++) {
//			cyclicShift(bytes, 1);
//			System.out.println(toBinaryString(bytes));
//		}
//		bytes = new byte[] { (byte) 0x81, 24, 7 };
//		cyclicShift(bytes, 9);
//		System.out.println(toBinaryString(bytes));
//		for (int i = 0; i < 9; i++) {
//			cyclicShift(bytes, -1);
//			System.out.println(toBinaryString(bytes));
//		}
	}

}
