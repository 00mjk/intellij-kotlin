var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , not:function(){
    {
      return 'hooray';
    }
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.A).not() == 'hooray';
  }
}
}, {A:classes.A});
foo.initialize();
