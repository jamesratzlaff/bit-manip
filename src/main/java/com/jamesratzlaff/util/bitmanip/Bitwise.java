/**
 * 
 */
package com.jamesratzlaff.util.bitmanip;

import static com.jamesratzlaff.util.bitmanip.internal.constants.Lambdas.ofType.IntBinaryOp.ADD;
import static com.jamesratzlaff.util.bitmanip.internal.constants.Lambdas.ofType.IntBinaryOp.GT;
import static com.jamesratzlaff.util.bitmanip.internal.constants.Lambdas.ofType.IntBinaryOp.LSH;
import static com.jamesratzlaff.util.bitmanip.internal.constants.Lambdas.ofType.IntBinaryOp.LT;
import static com.jamesratzlaff.util.bitmanip.internal.constants.Lambdas.ofType.IntBinaryOp.SUB;
import static com.jamesratzlaff.util.bitmanip.internal.constants.Lambdas.ofType.IntBinaryOp.URSH;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.jamesratzlaff.util.bitmanip.internal.constants.Lambdas;
import com.jamesratzlaff.util.bitmanip.internal.constants.Lambdas.forType.byteArray;
import com.jamesratzlaff.util.function.ByteBinaryOperator;
import com.jamesratzlaff.util.function.ObjIntByteConsumer;
import com.jamesratzlaff.util.function.ObjIntToByteBiFunction;

/**
 * @author James Ratzlaff
 *
 */
public class Bitwise {

	private abstract static class CyclicBytesOperator<T> {
		private final ToIntFunction<T> lengthGetter;
		private final ObjIntToByteBiFunction<T> getAtIndex;

		protected CyclicBytesOperator(ToIntFunction<T> lengthGetter, ObjIntToByteBiFunction<T> getAtIndex) {
			this.lengthGetter = lengthGetter;
			this.getAtIndex = getAtIndex;
		}

		protected ToIntFunction<T> lengthGetter() {
			return lengthGetter;
		}

		protected ObjIntToByteBiFunction<T> getAtIndex() {
			return getAtIndex;
		}
	}

	public static class CyclicReadBuffer<T> {
		private final T bytes;
		private final CyclicReadBufferController<T> controller;

		
		
		
		public CyclicReadBuffer(T bytes, CyclicReadBufferController<T> controller) {
			this.bytes = bytes;
			this.controller = controller;
		}

		public CyclicReadBuffer(T bytes, ToIntFunction<T> lengthGetter, ObjIntToByteBiFunction<T> getAtIndex) {
			this(bytes, new CyclicReadBufferController<>(lengthGetter, getAtIndex));
		}

		public byte get(long index) {
			return controller.get(bytes, index);
		}

		public byte get() {
			return controller.get(bytes);
		}

		public byte next() {
			byte b = controller.next(bytes);
			print((byte[]) bytes, (int) controller.location());
			return b;
		}

		public byte previous() {

			byte b = controller.previous(bytes);
			print((byte[]) bytes, (int) controller.location());
			return b;
		}

		public boolean hasNext() {
			return controller.hasNext();
		}

		public int actualSize() {
			return controller.actualSize(bytes);
		}
		
		public byte[] applyOperation(ByteBinaryOperator op, byte...opend) {
			return applyOperation(opend, byteArray.lengthGetter, byteArray.getAtIndex, byteArray.setAtIndex, op);
		}
		public byte[] applyOperation(IntBinaryOperator op, byte...opend) {
			return applyOperation(opend, byteArray.lengthGetter, byteArray.getAtIndex, byteArray.setAtIndex, op);
		}
		public ByteBuffer applyOperation(ByteBinaryOperator op, ByteBuffer opend) {
			return applyOperation(opend, ByteBuffer::limit, ByteBuffer::get, ByteBuffer::put, op);
		}
		public ByteBuffer applyOperation(IntBinaryOperator op, ByteBuffer opend) {
			return applyOperation(opend, ByteBuffer::limit, ByteBuffer::get, ByteBuffer::put, op);
		}
		
		public <U> U applyOperation(U opend, ToIntFunction<U> lengthGetter, ObjIntToByteBiFunction<U> getAtIndex, ObjIntByteConsumer<U> setAtIndex,ByteBinaryOperator op) {
			int len = lengthGetter.applyAsInt(opend);
			for(int i=0;i<len;i++) {
				byte a = getAtIndex.applyAsByte(opend, i);
				byte b = next();
				byte result = op.applyAsByte(a, b);
				setAtIndex.accept(opend,i, (byte)result);
			}
			return opend;
			
		}
		
		public <U> U applyOperation(U opend, ToIntFunction<U> lengthGetter, ObjIntToByteBiFunction<U> getAtIndex, ObjIntByteConsumer<U> setAtIndex,IntBinaryOperator op) {
			int len = lengthGetter.applyAsInt(opend);
			for(int i=0;i<len;i++) {
				int a = Byte.toUnsignedInt(getAtIndex.applyAsByte(opend, i));
				int b = Byte.toUnsignedInt(next());
				int result = op.applyAsInt(a, b);
				setAtIndex.accept(opend,i, (byte)result);
			}
			return opend;
		}

		public IntStream stream() {
			return IntStream.generate(this::next);
		}

		public static class from {
			public static final CyclicReadBuffer<byte[]> byteArray(byte[] bytes) {
				return new CyclicReadBuffer<byte[]>(bytes, Lambdas.forType.byteArray.lengthGetter, Lambdas.forType.byteArray.getAtIndex);
			}

			public static final CyclicReadBuffer<ByteBuffer> ByteBuffer(ByteBuffer bytes) {
				return new CyclicReadBuffer<ByteBuffer>(bytes, ByteBuffer::limit, ByteBuffer::get);
			}
		}
	}

	static class CyclicReadBufferController<T> extends CyclicBytesOperator<T> implements Cloneable {
		// TODO: maybe use an atomic long
		private int location = 0;

		public CyclicReadBufferController(ToIntFunction<T> lengthGetter, ObjIntToByteBiFunction<T> getAtIndex) {
			super(lengthGetter, getAtIndex);
		}

		int location() {
			return location;
		}

		public byte get(T bytes, long index) {
			int realLoc = getRealLocation(index, lengthGetter().applyAsInt(bytes));
			return getAtIndex().applyAsByte(bytes, realLoc);
		}

		protected int actualSize(T bytes) {
			return lengthGetter().applyAsInt(bytes);
		}

		public byte next(T bytes) {
			byte b = get(bytes);
			location += 1;
			if (location >= lengthGetter().applyAsInt(bytes)) {
				location = 0;
			}
			return b;
		}

		public byte previous(T bytes) {

			location -= 1;
			if (location < 0) {
				location = lengthGetter().applyAsInt(bytes) - 1;
			}
			return get(bytes);
		}

		private static int getRealLocation(long value, int len) {
			if (value < len && value > 0) {
				return (int) value;
			}
			long reso = 0;

			reso = value % Integer.toUnsignedLong(len);
			if (reso < 0) {
				reso = Integer.toUnsignedLong(len) - reso;
			}
			int result = (int) reso;
			if (result < 0) {
				throw new IllegalStateException(String.format("The resolved location (%s) should be a positive number, but it is not.  The arguments given are (value=%s,len=%s)", result, value, len));
			}
			return result;
		}

		public byte get(T bytes) {
			return getAtIndex().applyAsByte(bytes, location);
		}

		public boolean hasNext() {
			return true;
		}

		public CyclicReadBufferController<T> clone() {
			return new CyclicReadBufferController<>(lengthGetter(), getAtIndex());
		}

	}
	
	

	/**
	 * 
	 * @author James Ratzlaff
	 *
	 * @param <T> the type of object that has a length or size property, returns a byte value based on an index, and can store a value given an index
	 */
	public static class CyclicShifter<T> extends CyclicBytesOperator<T> {
		private final ObjIntByteConsumer<T> setAtIndex;

		public CyclicShifter(ToIntFunction<T> lengthGetter, ObjIntToByteBiFunction<T> getAtIndex, ObjIntByteConsumer<T> setAtIndex) {
			super(lengthGetter, getAtIndex);
			List<String> nullParams = getNullParams(new LinkedHashMap<String, Object>(Map.of("lengthGetter", lengthGetter(), "getAtIndex", getAtIndex(), "setAtIndex", setAtIndex)));
			if (!nullParams.isEmpty()) {
				throw new IllegalArgumentException(String.join(", ", nullParams) + " cannot be null");
			}
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

			int bytesLength = bytes != null ? lengthGetter().applyAsInt(bytes) : 0;
			if (amount != 0 && bytesLength > 0) {
				final int dataWidth = Byte.SIZE;
				final int startIdx = Math.max(Math.min(from, to), 0);
				final int endIdx = Math.min(Math.max(from, to), bytesLength);
				final int numberOfBytes = (endIdx - startIdx);
				if (numberOfBytes > 0) {
					amount = amount % (numberOfBytes * dataWidth);
					final IntBinaryOperator preLoopOp = amount<0 ? ADD : SUB;
					if (Math.abs(amount) > dataWidth) {
						final int chunkAmount = amount<0 ? -dataWidth : dataWidth;
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
					final IntBinaryOperator backShiftOp = iteratesForward ? LSH : URSH;
					final IntBinaryOperator shiftOp = iteratesForward ? URSH : LSH;
					int previousCarry = 0;
					final IntBinaryOperator endCondition = iteratesForward ? LT : GT;
					for (int i = start; endCondition.applyAsInt(i, end) == 1; i += direction) {
						previousCarry = manipulateBytesAndGetCarry(bytes, amount, backShift, previousCarry, i, carryMask, backShiftOp, shiftOp);
					}
					setAtIndex.accept(bytes, start, (byte) (getAtIndex().applyAsByte(bytes, start) | previousCarry));
				}
			}
		}

		private final int manipulateBytesAndGetCarry(final T bytes, int amount, final int backShift, int previousCarry, final int i, final int carryMask,
				final IntBinaryOperator backShiftOp, final IntBinaryOperator shiftOp) {
			int current = Byte.toUnsignedInt(getAtIndex().applyAsByte(bytes, i));
			int carry = (current & carryMask);
			carry = backShiftOp.applyAsInt(carry, backShift) & 0xFF;
			current = shiftOp.applyAsInt(current, amount) & 0xFF;
			setAtIndex.accept(bytes, i, (byte) (current | previousCarry));
			return carry;
		}

		private static final List<String> getNullParams(Map<String, ?> map) {
			return map.keySet().stream().filter(key -> map.get(key) == null).collect(Collectors.toList());
		}

		public static final class Using {
			public static final CyclicShifter<byte[]> aByteArray = new CyclicShifter<byte[]>(byteArray.lengthGetter, byteArray.getAtIndex, byteArray.setAtIndex);
			public static final CyclicShifter<ByteBuffer> aByteBuffer = new CyclicShifter<ByteBuffer>(ByteBuffer::limit, ByteBuffer::get, ByteBuffer::put);
		}

	}
	
	public static class Util {

		public static byte[] doOperation(byte[] opend, IntBinaryOperator op, byte...operand) {
			return CyclicReadBuffer.from.byteArray(operand).applyOperation(op, opend);
		}
		public static byte[] doOperation(byte[] opend, IntBinaryOperator op, ByteBuffer operand) {
			return CyclicReadBuffer.from.ByteBuffer(operand).applyOperation(op, opend);
		}
		public static ByteBuffer doOperation(ByteBuffer opend, IntBinaryOperator op, ByteBuffer operand) {
			return CyclicReadBuffer.from.ByteBuffer(operand).applyOperation(opend, ByteBuffer::limit,ByteBuffer::get,ByteBuffer::put, op);
		}
		public static ByteBuffer doOperation(ByteBuffer opend, IntBinaryOperator op, byte[] operand) {
			return CyclicReadBuffer.from.byteArray(operand).applyOperation(opend, ByteBuffer::limit,ByteBuffer::get,ByteBuffer::put, op);
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
		return String.join("", toStringList(bytes, Bitwise::asByteBinary));
	}

	public static List<String> toStringList(byte[] bytes, IntFunction<String> mapper) {
		return Arrays.stream(toIntArray(bytes)).mapToObj(mapper).collect(Collectors.toList());
	}

	public static List<String> createPointers(List<String> other, int index) {
		List<String> result = new ArrayList<String>(other.size());
		for (int i = 0; i < other.size(); i++) {
			String current = other.get(i);
			char[] aCarr = current.toCharArray();
			Arrays.fill(aCarr, i == index ? '^' : ' ');
			result.add(new String(aCarr));
		}
		return result;
	}

	public static void print(byte[] bytes, int index) {
		System.out.println(index);
		System.out.println(createPointedString(bytes, index));
	}

	public static String createPointedString(byte[] bytes, int index) {
		return createPointedString(bytes, index, "");
	}
	

	public static String createPointedString(byte[] bytes, int index, String delim) {
		List<String> asList = toStringList(bytes, Bitwise::asByteBinary);
		List<String> ptrs = createPointers(asList, index);
		char[] bdelim = delim.toCharArray();
		Arrays.fill(bdelim, ' ');
		String odelim = new String(bdelim);
		String m = String.join(delim, asList);
		String n = String.join(odelim, ptrs);
		return m + "\n" + n;
	}

	public static void main(String[] args) {
		byte[] bytes = new byte[] { (byte) 0x81, 24, 7 };
		System.out.println("shifted 0:  " + toBinaryString(bytes));
		System.out.println("shifting 1, 9 times");
		for (int i = 0; i < 9; i++) {
			CyclicShifter.Using.aByteArray.cyclicShift(bytes, 1);
			System.out.println("shifted 1:  " + toBinaryString(bytes));
		}
		System.out.println("shifting -9, 1 time");
		CyclicShifter.Using.aByteArray.cyclicShift(bytes, -9);
		System.out.println("shifted -9: " + toBinaryString(bytes));
		System.out.println("shifting 9, 1 time");
		System.out.println("shifted 9:  " + toBinaryString(CyclicShifter.Using.aByteArray.cyclicShiftAndReturnBytes(bytes, 9)));
		System.out.println("shifting -1, 9 times");
		for (int i = 0; i < 9; i++) {
			CyclicShifter.Using.aByteArray.cyclicShift(bytes, -1);
			System.out.println("shifted -1: " + toBinaryString(bytes));
		}
		CyclicReadBuffer<byte[]> readBuff = CyclicReadBuffer.from.byteArray(bytes);
		for (int i = 0; i < 7; i++) {

			readBuff.next();
		}

		bytes = new byte[] { (byte) 0x81, 24, 7 };
		System.err.println(toBinaryString(bytes));
		for (int i = 0; i < 13; i++) {
			System.out.println(readBuff.previous());
		}
		Util.doOperation(bytes, (a, b)->(a^b),(byte)255,(byte)0);
		System.err.println(toBinaryString(bytes));
	}

}
