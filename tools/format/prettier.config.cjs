const config = {
  tabWidth: 2,
  printWidth: 120,
  htmlWhitespaceSensitivity: "ignore",
  singleAttributePerLine: true,
  xmlSortAttributesByKey: true,
  xmlWhitespaceSensitivity: "ignore",
  plugins: [
    require.resolve("@prettier/plugin-xml")
  ],
};

module.exports = config;
