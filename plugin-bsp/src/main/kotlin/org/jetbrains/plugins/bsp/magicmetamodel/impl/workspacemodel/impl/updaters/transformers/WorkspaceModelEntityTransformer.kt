package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.WorkspaceModelEntity

internal interface WorkspaceModelEntityBaseTransformer<in T, out R> {
  fun transform(inputEntity: T): R
}

internal interface WorkspaceModelEntityTransformer<in T, out R : WorkspaceModelEntity> :
  WorkspaceModelEntityBaseTransformer<T, R> {
  fun transform(inputEntities: List<T>): List<R> =
    inputEntities.map { transform(it) }.distinct()

  override fun transform(inputEntity: T): R
}

internal interface WorkspaceModelEntityPartitionTransformer<in T, out R : WorkspaceModelEntity> :
  WorkspaceModelEntityBaseTransformer<T, List<R>> {
  fun transform(inputEntities: List<T>): List<R> =
    inputEntities.flatMap { transform(it) }.distinct()

  override fun transform(inputEntity: T): List<R>
}
