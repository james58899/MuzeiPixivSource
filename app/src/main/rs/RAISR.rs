#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(one.oktw.muzeipixivsource.renderscript)

uchar RS_KERNEL toGray(uchar4 input) {
    return input.r * 0.299 + input.g * 0.587 + input.b * 0.114;
}

short RS_KERNEL genPatch(uchar input, float GX, float GY, uint32_t x, uint32_t y) {
    return 0;
}

uchar4 RS_KERNEL applyPatch(uchar4 image, short patch) {
    image.r = clamp(image.r + patch, 0, 255);
    image.g = clamp(image.g + patch, 0, 255);
    image.b = clamp(image.b + patch, 0, 255);

    return image;
}

// void process(rs_allocation image) {
//     const uint32_t width = rsAllocationGetDimX(image);
//     const uint32_t heigth = rsAllocationGetDimY(image);
//
//     grayed = rsCreateAllocation_uchar(width, heigth);
//     rsForEach(toGray, image, grayed);
//
//     GX = rsCreateAllocation_float(width, heigth);
//     GY = rsCreateAllocation_float(width, heigth);
//     rsForEach(gradient, grayed);
//
//     patch = rsCreateAllocation_short(width, heigth);
//     rsForEach(genPatch, grayed, GX, GY, patch);
//
//     rsClearObject(&grayed);
//     rsClearObject(&GX);
//     rsClearObject(&GY);
//
//     rsForEach(applyPatch, image, patch, image);
// }
