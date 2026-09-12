<template>
  <el-tooltip :content="title" placement="bottom" effect="light">
    <div style="width: 30px;height: 20px;display: inline-block;">
      <div class="outer" :class="outerClass">
        <div class="round-background" :class="shadowClass" > </div>
      </div>
    </div>
  </el-tooltip>
</template>

<script>
export default {
  name: "color-solid-round",
  props: {
    title: {
      type: String,
      default: ""
    },
    color: {
      type: String,
      default: "red"
    },
    activeColor: {
      type: String,
      default: "red"
    }
  },
  // data: {
  //   return: {
  //     outerClass: 'outer-red'
  //   }
  // },
  computed: {
    outerClass() {
      if (this.activeColor == this.color) {
        return "outer-"+this.color+ " active-outer"
      }
      return  "outer-"+this.color
    },
    shadowClass() {
      return "round-background-"+this.color
    }
  },
  methods: {

  }
}
</script>

<style scoped>


.outer {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  transition: transform 0.5s;
  overflow: hidden;

  background: #e9ecef;
  color: #333;
  box-shadow:
      2px 2px 4px rgba(0, 0, 0, .4),
      -2px -2px 4px rgba(255, 255, 255, .9);

}


.outer:hover {
  cursor: pointer;
  transform: scale(1.3);
  transition: transform 0.5s;
}


.active-outer{
  transform: scale(1.3);
  transition: transform 0.5s;
}

.outer-red:hover {
  box-shadow: 0px 0px 5px 2px rgba(234, 98, 112, 0.9);
}

.round-background-red {
  background: #fd6458;width: 100%;height: 100%
}




.outer-green:hover {
  box-shadow: 0px 0px 5px 2px rgba(36,204,61, 0.9);
}

.round-background-green {
  background: #24cc3d;width: 100%;height: 100%
}



.outer-yellow:hover {
  box-shadow: 0px 0px 5px 2px rgba(255,191,43, 0.9);
}

.round-background-yellow {
  background: #ffbf2b;width: 100%;height: 100%
}

 /*box-shadow: 20px 0 0 #ffbf2b, 40px 0 0 #24cc3d, 60px 0 0 #FFFF00;*/

</style>
