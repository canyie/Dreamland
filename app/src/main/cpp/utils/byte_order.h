//
// Created by canyie on 2020/8/9.
//

#ifndef DREAMLAND_BYTE_ORDER_H
#define DREAMLAND_BYTE_ORDER_H

/**
 * Android only use little-endian.
 */

#define dtohl(x) (x)
#define dtohs(x) (x)
#define htodl(x) (x)
#define htods(x) (x)

#define fromlel(x) (x)
#define tolel(x) (x)

#endif //DREAMLAND_BYTE_ORDER_H
