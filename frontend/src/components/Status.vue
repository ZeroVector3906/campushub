<script setup lang="ts">import {label,statusType} from '../utils';defineProps<{value:string}>()</script>
<template><el-tag :type="statusType(value)" effect="light" size="small" round>{{label(value)}}</el-tag></template>
