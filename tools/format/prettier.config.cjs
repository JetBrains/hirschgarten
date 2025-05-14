const config = {
  tabWidth: 2,
  printWidth: 120,
  xmlWhitespaceSensitivity: "ignore",
  bracketSameLine: true,
  singleAttributePerLine: true,
  plugins: [
    require("@prettier/plugin-xml"),
  ],
};

module.exports = config;
