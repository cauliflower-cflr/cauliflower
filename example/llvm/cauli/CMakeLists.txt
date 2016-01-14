include_directories (${cauli_SOURCE_DIR}/include)

set (CauliSourceCodes
    andersen_cflr.cpp
    andersen.h
    dyck.cpp
)
add_library (Cauli SHARED ${CauliSourceCodes})

