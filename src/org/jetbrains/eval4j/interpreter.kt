package org.jetbrains.eval4j

import org.objectweb.asm.tree.analysis.*
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.Label

class UnsupportedByteCodeException(message: String) : RuntimeException(message)

trait Eval {
    fun loadClass(classType: Type): Value
    fun newInstance(classType: Type): Value
    fun checkCast(value: Value, targetType: Type): Value
    fun isInsetanceOf(value: Value, targetType: Type): Boolean

    fun newArray(arrayType: Type, size: Int): Value
    fun newMultiDimensionalArray(arrayType: Type, dimensionSizes: List<Int>): Value
    fun getArrayLength(array: Value): Value
    fun getArrayElement(array: Value, index: Value): Value
    fun setArrayElement(array: Value, index: Value, newValue: Value)

    fun getStaticField(fieldDesc: String): Value
    fun setStaticField(fieldDesc: String, newValue: Value)
    fun invokeStaticMethod(methodDesc: String, arguments: List<Value>): Value

    fun getField(instance: Value, fieldDesc: String): Value
    fun setField(instance: Value, fieldDesc: String, newValue: Value)
    fun invokeMethod(instance: Value, methodDesc: String, arguments: List<Value>, invokespecial: Boolean = false): Value
}

trait Control {
    fun jump(label: Label)

    fun returnValue(value: Value)
    fun throwException(value: Value)
}

class SingleInstructionInterpreter(private val eval: Eval, private val control: Control) : Interpreter<Value>(ASM4) {
    override fun newValue(`type`: Type?): Value? {
        if (`type` == null) {
            return NOT_A_VALUE
        }

        return when (`type`.getSort()) {
            Type.VOID -> null
            else -> NotInitialized(`type`)
        }
    }

    override fun newOperation(insn: AbstractInsnNode): Value? {
        return when (insn.getOpcode()) {
            ACONST_NULL -> {
                return NULL_VALUE
            }

            ICONST_M1 -> int(-1)
            ICONST_0 -> int(0)
            ICONST_1 -> int(1)
            ICONST_2 -> int(2)
            ICONST_3 -> int(3)
            ICONST_4 -> int(4)
            ICONST_5 -> int(5)

            LCONST_0 -> long(0)
            LCONST_1 -> long(1)

            FCONST_0 -> float(0.0)
            FCONST_1 -> float(1.0)
            FCONST_2 -> float(2.0)

            DCONST_0 -> double(0.0)
            DCONST_1 -> double(1.0)

            BIPUSH, SIPUSH -> int((insn as IntInsnNode).operand)

            LDC -> {
                val cst = ((insn as LdcInsnNode)).cst
                when (cst) {
                    is Int -> int(cst)
                    is Float -> float(cst)
                    is Long -> long(cst)
                    is Double -> double(cst)
                    is String -> obj(cst)
                    is Type -> {
                        val sort = (cst as Type).getSort()
                        when (sort) {
                            Type.OBJECT, Type.ARRAY -> eval.loadClass(cst)
                            Type.METHOD -> throw UnsupportedByteCodeException("Mothod handles are not supported")
                            else -> throw UnsupportedByteCodeException("Illegal LDC constant " + cst)
                        }
                    }
                    is Handle -> throw UnsupportedByteCodeException("Method handles are not supported")
                    else -> throw UnsupportedByteCodeException("Illegal LDC constant " + cst)
                }
            }
            JSR -> LabelValue((insn as JumpInsnNode).label.getLabel())
            GETSTATIC -> eval.getStaticField((insn as FieldInsnNode).desc)
            NEW -> eval.newInstance(Type.getType((insn as TypeInsnNode).desc))
            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    override fun copyOperation(insn: AbstractInsnNode, value: Value): Value {
        return value
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: Value): Value? {
        return when (insn.getOpcode()) {
            INEG -> int(-value.int)
            IINC -> int(value.int + (insn as IincInsnNode).incr)
            L2I -> int(value.long.toInt())
            F2I -> int(value.float.toInt())
            D2I -> int(value.double.toInt())
            I2B -> byte(value.int.toByte())
            I2C -> char(value.int.toChar())
            I2S -> short(value.int.toShort())

            FNEG -> float(-value.float)
            I2F -> float(value.int.toFloat())
            L2F -> float(value.long.toFloat())
            D2F -> float(value.double.toFloat())

            LNEG -> long(-value.long)
            I2L -> long(value.int.toLong())
            F2L -> long(value.float.toLong())
            D2L -> long(value.double.toLong())

            DNEG -> double(-value.double)
            I2D -> double(value.int.toDouble())
            L2D -> double(value.long.toDouble())
            F2D -> double(value.float.toDouble())

            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL -> {
                val label = (insn as JumpInsnNode).label.getLabel()
                when (insn.getOpcode()) {
                    IFEQ -> if (value.int == 0) control.jump(label)
                    IFNE -> if (value.int != 0) control.jump(label)
                    IFLT -> if (value.int < 0) control.jump(label)
                    IFGT -> if (value.int > 0) control.jump(label)
                    IFLE -> if (value.int <= 0) control.jump(label)
                    IFGE -> if (value.int >= 0) control.jump(label)
                    IFNULL -> if (value.obj == null) control.jump(label)
                    IFNONNULL -> if (value.obj != null) control.jump(label)
                }
                null
            }

            // TODO: switch
            TABLESWITCH,
            LOOKUPSWITCH -> throw UnsupportedByteCodeException("Switch is not supported yet")

            PUTSTATIC -> {
                eval.setStaticField((insn as FieldInsnNode).desc, value)
                null
            }

            GETFIELD -> eval.getField(value, (insn as FieldInsnNode).desc)

            NEWARRAY -> {
                val typeStr = when ((insn as IntInsnNode).operand) {
                    T_BOOLEAN -> "[Z"
                    T_CHAR    -> "[C"
                    T_BYTE    -> "[B"
                    T_SHORT   -> "[S"
                    T_INT     -> "[I"
                    T_FLOAT   -> "[F"
                    T_DOUBLE  -> "[D"
                    T_LONG    -> "[J"
                    else -> throw AnalyzerException(insn, "Invalid array type")
                }
                eval.newArray(Type.getType(typeStr), value.int)
            }
            ANEWARRAY -> {
                val desc = (insn as TypeInsnNode).desc
                eval.newArray(Type.getType("[" + Type.getObjectType(desc)), value.int)
            }
            ARRAYLENGTH -> eval.getArrayLength(value)

            ATHROW -> {
                control.throwException(value)
                null
            }

            CHECKCAST -> {
                val targetType = Type.getObjectType((insn as TypeInsnNode).desc)
                eval.checkCast(value, targetType)
            }

            INSTANCEOF -> {
                val targetType = Type.getObjectType((insn as TypeInsnNode).desc)
                boolean(eval.isInsetanceOf(value, targetType))
            }

            // TODO: maybe just do nothing?
            MONITORENTER, MONITOREXIT -> throw UnsupportedByteCodeException("Monitor instructions are not supported")

            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: Value, value2: Value): Value? {
        return when (insn.getOpcode()) {
            IALOAD, BALOAD, CALOAD, SALOAD,
            FALOAD, LALOAD, DALOAD,
            AALOAD -> eval.getArrayElement(value1, value2)

            IADD -> int(value1.int + value2.int)
            ISUB -> int(value1.int - value2.int)
            IMUL -> int(value1.int * value2.int)
            IDIV -> int(value1.int / value2.int)
            IREM -> int(value1.int % value2.int)
            ISHL -> int(value1.int shl value2.int)
            ISHR -> int(value1.int shr value2.int)
            IUSHR -> int(value1.int ushr value2.int)
            IAND -> int(value1.int and value2.int)
            IOR -> int(value1.int or value2.int)
            IXOR -> int(value1.int xor value2.int)

            LADD -> long(value1.long + value2.long)
            LSUB -> long(value1.long - value2.long)
            LMUL -> long(value1.long * value2.long)
            LDIV -> long(value1.long / value2.long)
            LREM -> long(value1.long % value2.long)
            LSHL -> long(value1.long shl value2.int)
            LSHR -> long(value1.long shr value2.int)
            LUSHR -> long(value1.long ushr value2.int)
            LAND -> long(value1.long and value2.long)
            LOR -> long(value1.long or value2.long)
            LXOR -> long(value1.long xor value2.long)

            FADD -> float(value1.float + value2.float)
            FSUB -> float(value1.float - value2.float)
            FMUL -> float(value1.float * value2.float)
            FDIV -> float(value1.float / value2.float)
            FREM -> float(value1.float % value2.float)

            DADD -> double(value1.double + value2.double)
            DSUB -> double(value1.double - value2.double)
            DMUL -> double(value1.double * value2.double)
            DDIV -> double(value1.double / value2.double)
            DREM -> double(value1.double % value2.double)

            LCMP -> {
                val l1 = value1.long
                val l2 = value2.long

                int(when {
                    l1 > l2 -> 1
                    l1 == l2 -> 0
                    else -> -1
                })
            }

            FCMPL,
            FCMPG -> {
                val l1 = value1.float
                val l2 = value2.float

                int(when {
                    l1 > l2 -> 1
                    l1 == l2 -> 0
                    l1 < l2 -> -1
                    // one of them is NaN
                    else -> if (insn.getOpcode() == FCMPG) 1 else -1
                })
            }

            DCMPL,
            DCMPG -> {
                val l1 = value1.double
                val l2 = value2.double

                int(when {
                    l1 > l2 -> 1
                    l1 == l2 -> 0
                    l1 < l2 -> -1
                    // one of them is NaN
                    else -> if (insn.getOpcode() == DCMPG) 1 else -1
                })
            }

            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> {
                val label = (insn as JumpInsnNode).label.getLabel()
                when (insn.getOpcode()) {
                    IF_ICMPEQ -> if (value1.int == value2.int) control.jump(label)
                    IF_ICMPNE -> if (value1.int != value2.int) control.jump(label)
                    IF_ICMPLT -> if (value1.int < value2.int) control.jump(label)
                    IF_ICMPGT -> if (value1.int > value2.int) control.jump(label)
                    IF_ICMPLE -> if (value1.int <= value2.int) control.jump(label)
                    IF_ICMPGE -> if (value1.int >= value2.int) control.jump(label)

                    IF_ACMPEQ -> if (value1.obj == value2.obj) control.jump(label)
                    IF_ACMPNE -> if (value1.obj != value2.obj) control.jump(label)
                }
                null
            }

            PUTFIELD -> {
                eval.setField(value1, (insn as FieldInsnNode).desc, value2)
                null
            }

            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    override fun ternaryOperation(insn: AbstractInsnNode, value1: Value, value2: Value, value3: Value): Value? {
        return when (insn.getOpcode()) {
            IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE -> {
                eval.setArrayElement(value1, value2, value3)
                null
            }
            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    override fun naryOperation(insn: AbstractInsnNode, values: List<Value>): Value {
        return when (insn.getOpcode()) {
            MULTIANEWARRAY -> {
                val node = insn as MultiANewArrayInsnNode
                eval.newMultiDimensionalArray(Type.getType(node.desc), values.map { v -> v.int })
            }

            INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE -> {
                val desc = (insn as MethodInsnNode).desc
                eval.invokeMethod(
                        values[0],
                        desc,
                        values.subList(1, values.size()),
                        insn.getOpcode() == INVOKESPECIAL
                )
            }

            INVOKESTATIC -> eval.invokeStaticMethod((insn as MethodInsnNode).desc, values)

            INVOKEDYNAMIC -> throw UnsupportedByteCodeException("INVOKEDYNAMIC is not supported")
            else -> throw UnsupportedByteCodeException("$insn")
        }
    }


    override fun returnOperation(insn: AbstractInsnNode, value: Value, expected: Value) {
        when (insn.getOpcode()) {
            IRETURN,
            LRETURN,
            FRETURN,
            DRETURN,
            ARETURN -> {
                // TODO: coercion, maybe?
                control.returnValue(value)
            }

            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    override fun merge(v: Value, w: Value): Value {
        // We always remember the NEW value
        return w
    }
}
