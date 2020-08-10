//
// Created by canyie on 2019/11/18.
//

#ifndef DREAMLAND_MACROS_H
#define DREAMLAND_MACROS_H

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

#define LIKELY(x) __builtin_expect(!!(x), 1)
#define UNLIKELY(x) __builtin_expect(!!(x), 0)

#ifdef __LP64__
#define LP_SELECT(lp64, lp32) (lp64)
#else
#define LP_SELECT(lp64, lp32) (lp32)
#endif

// From Android Open Source Project:

// A macro to disallow the copy constructor and operator= functions
// This must be placed in the private: declarations for a class.
//
// For disallowing only assign or copy, delete the relevant operator or
// constructor, for example:
// void operator=(const TypeName&) = delete;
// Note, that most uses of DISALLOW_ASSIGN and DISALLOW_COPY are broken
// semantically, one should either use disallow both or neither. Try to
// avoid these in new code.
#define DISALLOW_COPY_AND_ASSIGN(TypeName) \
TypeName(const TypeName&) = delete;      \
void operator=(const TypeName&) = delete

// A macro to disallow all the implicit constructors, namely the
// default constructor, copy constructor and operator= functions.
//
// This should be used in the private: declarations for a class
// that wants to prevent anyone from instantiating it. This is
// especially useful for classes containing only static methods.
#define DISALLOW_IMPLICIT_CONSTRUCTORS(TypeName) \
TypeName() = delete;                           \
DISALLOW_COPY_AND_ASSIGN(TypeName)

#endif //DREAMLAND_MACROS_H
