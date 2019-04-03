# bit-manip
Library for bitwise manipulations over arrays
#Examples

```java
//Given an array or (buffer) of bytes:
byte[] bytes =new byte[]{0b10000001,0b00011000,0b00000111};//100000010001100000000111
//Then invoking a bitwise cyclic right shift 2
Bitwise.CyclicShifter.Using.aByteArray(bytes,2);
//the contents of bytes is now 
//{0b11100000,0b010001100,0b0000000}                         111000000100011000000001
//Invoking a bitwise cyclic left shift 2
Bitwise.CyclicShifter.Using.aByteArray(bytes,-2);
//bytes is back to {0b10000001,0b00011000,0b0000111};        100000010001100000000111
//Then invoking a bitwise cyclic right shift 9
Bitwise.CyclicShifter.Using.aByteArray(bytes,9);
//the contents of bytes will be 
//{0b00000011,0b11000000,0b10001100}                         000000111100000010001100
```
There is also the method ``Bitwise.CyclicShifter.Using.aByteBuffer`` implemented for you as well as a generic constructor ``new CyclicShifter<T>(ToIntFunction<T> lengthGetter, ObjIntToByteFunction<T> getAtIndex, ObjIntByteConsumer<T> setAtIndex)``
to easily use whatever kind of byte container you please. This uses bitwise operations itself to achieve this so it runs pretty darn quick.
Another object you may find useful is a ``CyclicByteBuffer`` which can be used to do operations over arrays smaller or larger than itself. A use of this object can be found in the ``Bitwise.Util.doOperation(byte[],IntBinaryOperator,byte...)`` method.


Example

```java
byte[] bytes =new byte[]{0b10000001,0b00011000,0b00000111};//100000010001100000000111
//Let's XOR these bytes with {0b11111111,0b00000000}
Util.doOperation(bytes, (a, b)->(a^b),(byte)-1,(byte)0);  
//the contents of bytes will now be
//{0b01111110,0b00011000,0b11111000}                         011111100001100011111000
```


