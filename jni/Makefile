HEADER = ./supergame_physics_NativePhysics.h
LIB = ../lib/native/macosx/libNativePhysics.dylib

$(LIB) : $(HEADER) nativePhysics.cpp
	g++ -shared -I/System/Library/Frameworks/JavaVM.Framework/Headers -I../include/bullet \
		-o $(LIB) nativePhysics.cpp -framework JavaVM \
		-L../lib/native/macosx -lBulletDynamics -lBulletCollision -lLinearMath

$(HEADER) : ../src/supergame/physics/Physics.java
	javah -jni -classpath ../bin supergame.physics.NativePhysics


.PHONY: all clean

all : $(LIB)

clean :
	rm $(HEADER) $(LIB)
