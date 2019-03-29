/**
 * 
 */
package com.jamesratzlaff.util.bitmanip;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.jamesratzlaff.util.function.ObjIntByteConsumer;
import com.jamesratzlaff.util.function.ObjIntToByteBiFunction;

/**
 * @author James Ratzlaff
 *
 */
public class Bitwise {

	public static final class Constants {
		private static final class Lambdas {
			private static final class ofType {
				private static final class IntBinOp {
					private static final IntBinaryOperator aLessThanB = (a, b) -> a < b ? 1 : 0;
					private static final IntBinaryOperator aGreaterThanB = (a, b) -> a > b ? 1 : 0;
					private static final IntBinaryOperator unsignedRightShift = (a, b) -> a >>> b;
					private static final IntBinaryOperator leftShift = (a, b) -> a << b;
					private static final IntBinaryOperator add = (a, b) -> a + b;
					private static final IntBinaryOperator sub = (a, b) -> a - b;
				}
			}

			private static final class forType {
				private static final class byteArray {
					private static final ToIntFunction<byte[]> lengthGetter = bytes -> bytes.length;
					private static final ObjIntToByteBiFunction<byte[]> getAtIndex = (bytes, i) -> bytes[i];
					private static final ObjIntByteConsumer<byte[]> setAtIndex = (bytes, i, b) -> bytes[i] = b;
				}
			}
		}

	}

	public static class CyclicShifter<T> {
		private final ToIntFunction<T> lengthGetter;
		private final ObjIntToByteBiFunction<T> getAtIndex;
		private final ObjIntByteConsumer<T> setAtIndex;

		public CyclicShifter(ToIntFunction<T> lengthGetter, ObjIntToByteBiFunction<T> getAtIndex, ObjIntByteConsumer<T> setAtIndex) {
			List<String> nullParams = getNullParams(new LinkedHashMap<String, Object>(Map.of("lengthGetter", lengthGetter, "getAtIndex", getAtIndex, "setAtIndex", setAtIndex)));
			if (!nullParams.isEmpty()) {
				throw new IllegalArgumentException(String.join(", ", nullParams) + " cannot be null");
			}
			this.lengthGetter = lengthGetter;
			this.getAtIndex = getAtIndex;
			this.setAtIndex = setAtIndex;
		}

		/**
		 * @param <T>    T Convenience method for calling {@link #cyclicShiftAndReturnBytes(Object, int, int)
		 *               cyclicShiftAndReturnBytes(bytes, amount, from)} using 0 as the value for {@code from}
		 * @param bytes  the bytes in which the bits are to be shifted
		 * @param amount the number of shifts to occur. If amount is less than 0 then a left cyclic shift will be executed,
		 *               other wise a right cyclic shift will be executed.
		 * @param from   the inclusive starting index of bytes to be shifted
		 * 
		 * @return the given {@code bytes} argument
		 */
		public T cyclicShiftAndReturnBytes(T bytes, int amount) {
			return cyclicShiftAndReturnBytes(bytes, amount, 0);
		}

		/**
		 * 
		 * @param <T>    T
		 * @param bytes
		 * @param amount
		 * @param from
		 * @return the given {@code bytes} argument
		 */
		public T cyclicShiftAndReturnBytes(T bytes, int amount, int from) {
			return cyclicShiftAndReturnBytes(bytes, amount, from, Integer.MAX_VALUE);
		}

		/**
		 * @param <T>    T
		 * @param bytes  the bytes in which the bits are to be shifted
		 * @param amount the number of shifts to occur. If amount is less than 0 then a left cyclic shift will be executed,
		 *               other wise a right cyclic shift will be executed.
		 * @param from   the inclusive starting index of bytes to be shifted
		 * @param to     the exclusive end index of bytes to be shifted
		 * @return the given {@code bytes} argument
		 */
		public T cyclicShiftAndReturnBytes(T bytes, int amount, int from, int to) {
			cyclicShift(bytes, amount, from, to);
			return bytes;
		}

		/**
		 * @param <T>    Convenience method for calling {@link #cyclicShift(Object, int, int) cyclicShift(bytes, amount, from)}
		 *               using 0 as the value for {@code from}
		 * @param bytes  the bytes in which the bits are to be shifted
		 * @param amount the number of shifts to occur. If amount is less than 0 then a left cyclic shift will be executed,
		 *               other wise a right cyclic shift will be executed.
		 * @param from   the inclusive starting index of bytes to be shifted
		 */
		public void cyclicShift(T bytes, int amount) {
			cyclicShift(bytes, amount, 0);
		}

		/**
		 * Convenience method for calling {@link #cyclicShift(Object, int, int, int) cyclicShift(bytes, amount, from, to)} using
		 * {@code bytes'} max-index/length value as the value for {@code to}
		 * 
		 * @param bytes  the bytes in which the bits are to be shifted
		 * @param amount the number of shifts to occur. If amount is less than 0 then a left cyclic shift will be executed,
		 *               other wise a right cyclic shift will be executed.
		 * @param from   the inclusive starting index of bytes to be shifted
		 */
		public void cyclicShift(T bytes, int amount, int from) {
			cyclicShift(bytes, amount, from, Integer.MAX_VALUE);
		}

		/**
		 * 
		 * @param bytes  the bytes in which the bits are to be shifted
		 * @param amount the number of shifts to occur. If amount is less than 0 then a left cyclic shift will be executed,
		 *               other wise a right cyclic shift will be executed.
		 * @param from   the inclusive starting index of bytes to be shifted
		 * @param to     the exclusive end index of bytes to be shifted
		 */
		public void cyclicShift(T bytes, int amount, int from, int to) {

			int bytesLength = bytes != null ? lengthGetter.applyAsInt(bytes) : 0;
			if (amount != 0 && bytesLength > 0) {
				final int dataWidth = Byte.SIZE;
				final int startIdx = Math.max(Math.min(from, to), 0);
				final int endIdx = Math.min(Math.max(from, to), bytesLength);
				final int numberOfBytes = (endIdx - startIdx);
				if (numberOfBytes > 0) {
					amount = amount % (numberOfBytes * dataWidth);
					final IntBinaryOperator preLoopOp = amount < 0 ? Constants.Lambdas.ofType.IntBinOp.add : Constants.Lambdas.ofType.IntBinOp.sub;
					if (Math.abs(amount) > dataWidth) {
						final int chunkAmount = amount<0?-dataWidth:dataWidth;
						while (Math.abs(amount) > dataWidth) {
							cyclicShift(bytes, chunkAmount, startIdx, endIdx);
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
					final IntBinaryOperator backShiftOp = iteratesForward ? Constants.Lambdas.ofType.IntBinOp.leftShift : Constants.Lambdas.ofType.IntBinOp.unsignedRightShift;
					final IntBinaryOperator shiftOp = iteratesForward ? Constants.Lambdas.ofType.IntBinOp.unsignedRightShift : Constants.Lambdas.ofType.IntBinOp.leftShift;
					int previousCarry = 0;
					final IntBinaryOperator endCondition = iteratesForward ? Constants.Lambdas.ofType.IntBinOp.aLessThanB : Constants.Lambdas.ofType.IntBinOp.aGreaterThanB;
					for (int i = start; endCondition.applyAsInt(i, end) == 1; i += direction) {
						previousCarry = manipulateBytesAndGetCarry(bytes, amount, backShift, previousCarry, i, carryMask, backShiftOp, shiftOp);
					}
					setAtIndex.accept(bytes, start, (byte) (getAtIndex.applyAsByte(bytes, start) | previousCarry));
				}
			}
		}

		private final int manipulateBytesAndGetCarry(final T bytes, int amount, final int backShift, int previousCarry, final int i, final int carryMask,
				final IntBinaryOperator backShiftOp, final IntBinaryOperator shiftOp) {
			int current = Byte.toUnsignedInt(getAtIndex.applyAsByte(bytes, i));
			int carry = (current & carryMask);
			carry = backShiftOp.applyAsInt(carry, backShift) & 0xFF;
			current = shiftOp.applyAsInt(current, amount) & 0xFF;
			setAtIndex.accept(bytes, i, (byte) (current | previousCarry));
			return carry;
		}

		private static final List<String> getNullParams(Map<String, ?> map) {
			return map.keySet().stream().filter(key -> map.get(key) == null).collect(Collectors.toList());
		}

		public static final class ofType {
			public static final CyclicShifter<byte[]> byteArray = new CyclicShifter<byte[]>(Constants.Lambdas.forType.byteArray.lengthGetter, Constants.Lambdas.forType.byteArray.getAtIndex, Constants.Lambdas.forType.byteArray.setAtIndex);
			public static final CyclicShifter<ByteBuffer> ByteBuffer = new CyclicShifter<ByteBuffer>(java.nio.ByteBuffer::limit, java.nio.ByteBuffer::get, java.nio.ByteBuffer::put);
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

	public static String toBinaryString(String label, byte b) {
		return label + toBinaryString(b);
	}

	public static String toBinaryString(byte b) {
		return asByteBinary(Byte.toUnsignedInt(b));
	}

	public static String toBinaryString(byte[] bytes) {
		return String.join("", Arrays.stream(toIntArray(bytes)).mapToObj(Bitwise::asByteBinary).collect(Collectors.toList()));
	}

	public static void main(String[] args) {
		byte[] bytes = new byte[] { (byte) 0x81, 24, 7 };
		System.out.println("shifted 0:  "+toBinaryString(bytes));
		System.out.println("shifting 1, 9 times");
		for (int i = 0; i < 9; i++) {
			CyclicShifter.ofType.byteArray.cyclicShift(bytes, 1);
			System.out.println("shifted 1:  "+toBinaryString(bytes));
		}
		System.out.println("shifting -9, 1 time");
		CyclicShifter.ofType.byteArray.cyclicShift(bytes, -9);
		System.out.println("shifted -9: "+toBinaryString(bytes));
		System.out.println("shifting 9, 1 time");
		System.out.println("shifted 9:  "+toBinaryString(CyclicShifter.ofType.byteArray.cyclicShiftAndReturnBytes(bytes, 9)));
		System.out.println("shifting -1, 9 times");
		for (int i = 0; i < 9; i++) {
			CyclicShifter.ofType.byteArray.cyclicShift(bytes, -1);
			System.out.println("shifted -1: "+toBinaryString(bytes));
		}
	}

}
