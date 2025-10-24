package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelEntity

interface WorkspaceModelEntityBaseTransformer<in T, out R> {
  fun transform(inputEntity: T): R
}

interface WorkspaceModelEntityTransformer<in T, out R : WorkspaceModelEntity> : WorkspaceModelEntityBaseTransformer<T, R> {
  fun transform(inputEntities: List<T>): List<R> = inputEntities.map { transform(it) }.distinct()

  override fun transform(inputEntity: T): R
}

interface WorkspaceModelEntityPartitionTransformer<in T, out R : WorkspaceModelEntity> :
  WorkspaceModelEntityBaseTransformer<T, List<R>> {
  fun transform(inputEntities: List<T>): List<R> = inputEntities.flatMap { transform(it) }.distinct()

  override fun transform(inputEntity: T): List<R>
}
