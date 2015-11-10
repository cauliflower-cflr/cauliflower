# Executables
MAIN=main
TEST=test

# Code directories
SRC_DIR=src
TST_DIR=test
SPK_DIR=spikes
INCLUDES=-I include/

# Options and Flags
#CXXFLAGS=$(INCLUDES) -Wl,-rpath -Wl,/usr/local/lib -std=c++11 -Wall -g
CXXFLAGS=$(INCLUDES) -std=c++11 -Wall -O3
CXXLIBS=#-llog4cplus
TEST_ARGS=--report_level=detailed

# Build directories
BIN_DIR=bin
BUILD_DIR=build
DEP_DIR=$(BUILD_DIR)/deps
OBJ_DIR=$(BUILD_DIR)/objs

SRCS=$(wildcard $(SRC_DIR)/*.cpp)
TSTS=$(wildcard $(TST_DIR)/*.cpp)
SPKS=$(wildcard $(SPK_DIR)/*.cpp)
ALL_CODE=$(SRCS) $(TSTS) $(SPKS)
OBJS=$(patsubst %.cpp,$(OBJ_DIR)/%.o,$(ALL_CODE))
DEPS=$(patsubst %.cpp,$(DEP_DIR)/%.P,$(ALL_CODE))
OBJ_S=$(patsubst %.cpp,$(OBJ_DIR)/%.o,$(filter-out $(SRC_DIR)/main.cpp, $(SRCS)))
OBJ_T=$(patsubst %.cpp,$(OBJ_DIR)/%.o,$(filter-out $(TST_DIR)/test.cpp, $(TSTS)))

TARGETS=$(patsubst $(SPK_DIR)/%.cpp,%,$(SPKS)) $(MAIN) $(TEST)
BINS=$(patsubst %,$(BIN_DIR)/%,$(TARGETS))

.PHONY: clean all test

all: $(OBJS) $(BINS)

test: all
	$(BIN_DIR)/$(TEST) $(TEST_ARGS)

$(BIN_DIR)/$(MAIN): $(OBJ_DIR)/$(SRC_DIR)/$(MAIN).o $(OBJ_S)
	@[ -d $(BIN_DIR) ] || mkdir -p $(BIN_DIR)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(CXXLIBS)

$(BIN_DIR)/$(TEST): $(OBJ_DIR)/$(TST_DIR)/$(TEST).o $(OBJ_S) $(OBJ_T)
	@[ -d $(BIN_DIR) ] || mkdir -p $(BIN_DIR)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(CXXLIBS)

$(BIN_DIR)/%: $(OBJ_DIR)/$(SPK_DIR)/%.o $(OBJ_S)
	@[ -d $(BIN_DIR) ] || mkdir -p $(BIN_DIR)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(CXXLIBS)

$(OBJ_DIR)/%.o : %.cpp
	@[ -d $(DEP_DIR)/`dirname $<` ] || mkdir -p $(DEP_DIR)/`dirname $<` $(OBJ_DIR)/`dirname $<`
	$(CXX) $(CXXFLAGS) -MMD -c -o $@ $< $(CXXLIBS)
	@cp $(OBJ_DIR)/$*.d $(DEP_DIR)/$*.P; sed -e 's/#.*//' -e 's/^[^:]*: *//' -e 's/ *\\$$//' -e '/^$$/ d' -e 's/$$/ :/' < $(OBJ_DIR)/$*.d >> $(DEP_DIR)/$*.P; rm -f $(OBJ_DIR)/$*.d

clean:
	rm -rf $(DEP_DIR) $(OBJ_DIR) $(BUILD_DIR) $(BIN_DIR)

-include $(DEPS)
