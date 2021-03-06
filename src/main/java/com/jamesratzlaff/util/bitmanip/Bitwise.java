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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.jamesratzlaff.util.bitmanip.internal.constants.Lambdas;
import com.jamesratzlaff.util.bitmanip.internal.constants.Lambdas.forType.byteArray;
import com.jamesratzlaff.util.function.ByteBinaryOperator;
import com.jamesratzlaff.util.function.ObjIntByteConsumer;
import com.jamesratzlaff.util.function.ObjIntToByteFunction;

/**
 * @author James Ratzlaff
 *
 */
public class Bitwise {

	private abstract static class CyclicBytesOperator<T> {
		private final ToIntFunction<T> lengthGetter;
		private final ObjIntToByteFunction<T> getAtIndex;

		protected CyclicBytesOperator(ToIntFunction<T> lengthGetter, ObjIntToByteFunction<T> getAtIndex) {
			this.lengthGetter = lengthGetter;
			this.getAtIndex = getAtIndex;
		}

		protected ToIntFunction<T> lengthGetter() {
			return lengthGetter;
		}

		protected ObjIntToByteFunction<T> getAtIndex() {
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

		public CyclicReadBuffer(T bytes, ToIntFunction<T> lengthGetter, ObjIntToByteFunction<T> getAtIndex) {
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
			return b;
		}

		public byte previous() {
			byte b = controller.previous(bytes);
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
		
		public <U> U applyOperation(U opend, ToIntFunction<U> lengthGetter, ObjIntToByteFunction<U> getAtIndex, ObjIntByteConsumer<U> setAtIndex,ByteBinaryOperator op) {
			int len = lengthGetter.applyAsInt(opend);
			for(int i=0;i<len;i++) {
				byte a = getAtIndex.applyAsByte(opend, i);
				byte b = next();
				byte result = op.applyAsByte(a, b);
				setAtIndex.accept(opend,i, (byte)result);
			}
			return opend;
			
		}
		
		public <U> U applyOperation(U opend, ToIntFunction<U> lengthGetter, ObjIntToByteFunction<U> getAtIndex, ObjIntByteConsumer<U> setAtIndex,IntBinaryOperator op) {
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

		public CyclicReadBufferController(ToIntFunction<T> lengthGetter, ObjIntToByteFunction<T> getAtIndex) {
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

		private static <T> void checkForNullCtorArgs(ToIntFunction<T> lengthGetter, ObjIntToByteFunction<T> getAtIndex, ObjIntByteConsumer<T> setAtIndex) {
			Map<String,Object> mappedArgs = new LinkedHashMap<String,Object>(3);
			mappedArgs.put("lengthGetter", lengthGetter);
			mappedArgs.put("getAtIndex", getAtIndex);
			mappedArgs.put("setAtIndex", setAtIndex);
			List<String> nullParams = getNullParams(mappedArgs);
			if (!nullParams.isEmpty()) {
				throw new IllegalArgumentException(String.join(", ", nullParams) + " cannot be null");
			}
		}
		
		public CyclicShifter(ToIntFunction<T> lengthGetter, ObjIntToByteFunction<T> getAtIndex, ObjIntByteConsumer<T> setAtIndex) {
			super(lengthGetter, getAtIndex);
			checkForNullCtorArgs(lengthGetter, getAtIndex, setAtIndex);
			this.setAtIndex = setAtIndex;
		}

		/**
		 * Convenience method for calling {@link #cyclicShiftAndReturnBytes(Object, int, int)
		 *               cyclicShiftAndReturnBytes(bytes, amount, from)} using 0 as the value for {@code from}
		 * @param bytes  the bytes in which the bits are to be shifted
		 * @param amount the number of shifts to occur. If amount is less than 0 then a left cyclic shift will be executed,
		 *               other wise a right cyclic shift will be executed.
		 * 
		 * @return the given {@code bytes} argument
		 */
		public T cyclicShiftAndReturnBytes(T bytes, int amount) {
			return cyclicShiftAndReturnBytes(bytes, amount, 0);
		}

		/**
		 * 
		 * @param bytes the bytes in which the bits are to be shifted
		 * @param amount the number of shifts to occur. If amount is less than 0 then a left cyclic shift will be executed,
		 *               other wise a right cyclic shift will be executed.
		 * @param from the inclusive starting index of bytes to be shifted
		 * @return the given {@code bytes} argument
		 */
		public T cyclicShiftAndReturnBytes(T bytes, int amount, int from) {
			return cyclicShiftAndReturnBytes(bytes, amount, from, Integer.MAX_VALUE);
		}

		/**
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
		 *     Convenience method for calling {@link #cyclicShift(Object, int, int) cyclicShift(bytes, amount, from)}
		 *               using 0 as the value for {@code from}
		 * @param bytes  the bytes in which the bits are to be shifted
		 * @param amount the number of shifts to occur. If amount is less than 0 then a left cyclic shift will be executed,
		 *               other wise a right cyclic shift will be executed.
		 *
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

}
