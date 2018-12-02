#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(one.oktw.muzeipixivsource.renderscript)

static rs_allocation input;
static rs_allocation GX;
static rs_allocation GY;

void RS_KERNEL _gradient(uchar in, uint32_t x, uint32_t y) {
    // X
    if (x == 0) {
        uchar x0 = rsGetElementAt_uchar(input, 0, y);
        uchar x1 = rsGetElementAt_uchar(input, 1, y);

        rsSetElementAt_float(GX, x1 - x0, x, y);
    } else if (x == rsAllocationGetDimX(input)) {
        float x0 = rsGetElementAt_uchar(input, x - 1, y);
        float x1 = rsGetElementAt_uchar(input, x, y);

        rsSetElementAt_float(GX, x1 - x0, x, y);
    } else {
        float x0 = rsGetElementAt_uchar(input, x - 1, y);
        float x1 = rsGetElementAt_uchar(input, x + 1, y);

        rsSetElementAt_float(GX, (x1 - x0) / 2, x, y);
    }

    // Y
    if (y == 0) {
        float y0 = rsGetElementAt_uchar(input, x, 0);
        float y1 = rsGetElementAt_uchar(input, x, 1);

        rsSetElementAt_float(GY, y1 - y0, x, y);
    } else if (y == rsAllocationGetDimY(input)) {
        float y0 = rsGetElementAt_uchar(input, x, y - 1);
        float y1 = rsGetElementAt_uchar(input, x, y);

        rsSetElementAt_float(GY, y1 - y0, x, y);
    } else {
        float y0 = rsGetElementAt_uchar(input, x, y - 1);
        float y1 = rsGetElementAt_uchar(input, x, y + 1);

        rsSetElementAt_float(GY, (y1 - y0) / 2, x, y);
    }
}

void gradient(rs_allocation image, rs_allocation outputX, rs_allocation outputY) {
     input = image;
     GX = outputX;
     GY = outputY;

     rsForEach(_gradient, input);
 }
