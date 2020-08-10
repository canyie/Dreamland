//
// Created by canyie on 2020/8/5.
//

#ifndef DREAMLAND_SCOPED_ELF_H
#define DREAMLAND_SCOPED_ELF_H

#include "macros.h"
#include "../pine.h"

namespace dreamland {
    class ScopedElf {
    public:
        ScopedElf(const char* elf) : handle_(PineOpenElf(elf)) {
        }

        ~ScopedElf() {
            PineCloseElf(handle_);
        }

        bool GetSymbolAddress(const char* symbol, void** out) {
            *out = PineGetElfSymbolAddress(handle_, symbol);
            return *out != nullptr;
        }

    private:
        void* handle_;

        DISALLOW_COPY_AND_ASSIGN(ScopedElf);
    };
}

#endif //DREAMLAND_SCOPED_ELF_H
