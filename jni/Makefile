LIB = ../lib/native/macosx/libNativePhysics.dylib

$(LIB) : nativePhysics.cpp
	g++ -shared -I/System/Library/Frameworks/JavaVM.Framework/Headers -I/Users/ccraik/play/bullet/include/bullet \
		-o ../lib/native/macosx/libNativePhysics.dylib nativePhysics.cpp -framework JavaVM \
		-L../lib/native/macosx -lBulletDynamics -lBulletCollision -lLinearMath

all : $(LIB)


clean :
	rm $(LIB)
