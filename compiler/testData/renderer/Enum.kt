package rendererTest

private enum class TheEnum(val rgb : Int) {
    VAL1 : TheEnum(0xFF0000)
}

//package rendererTest defined in root package
//private final enum class TheEnum : jet.Enum<rendererTest.TheEnum> defined in rendererTest
//private ctor TheEnum(rgb : jet.Int) defined in rendererTest.TheEnum
//value-parameter val rgb : jet.Int defined in rendererTest.TheEnum.<init>
//private final enum entry VAL1 : rendererTest.TheEnum defined in rendererTest.TheEnum.<class-object-for-TheEnum>
//private ctor VAL1() defined in rendererTest.TheEnum.<class-object-for-TheEnum>.VAL1
//public final val VAL1 : rendererTest.TheEnum defined in rendererTest.TheEnum.<class-object-for-TheEnum>