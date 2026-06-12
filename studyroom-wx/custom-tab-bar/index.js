Component({
  data: {
    selected: 0,
    color: "rgba(0,0,0,0.45)",
    selectedColor: "#1C8FFA",
    list: [
      { pagePath: "pages/home/index", text: "首页" },
      { pagePath: "pages/application/index", text: "入座" },
      { pagePath: "pages/my/index", text: "我的" },
    ],
  },
  methods: {
    switchTab(e) {
      const idx = Number(e.currentTarget.dataset.index);
      const item = this.data.list[idx];
      if (!item) return;
      const path = item.pagePath.startsWith("/") ? item.pagePath : "/" + item.pagePath;
      wx.switchTab({ url: path });
    },
  },
});
